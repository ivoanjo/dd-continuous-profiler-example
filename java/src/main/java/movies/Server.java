package movies;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.Random;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import datadog.trace.api.Trace;
import spark.Request;
import spark.Response;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;
import com.mongodb.MongoClientSettings;

import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import com.mongodb.client.model.Sorts;
import java.util.Arrays;
import java.util.function.Consumer;
import org.bson.Document;

import java.util.Arrays;

import java.nio.file.Path;


public class Server {
	private static final Gson GSON;
	// Problem: you parse Movies, Ratings, Keywords, and Credits all the time
	//private static volatile Supplier<List<Movie>> MOVIES = () -> Movie.getAll();
	private static volatile Supplier<List<Rating>> RATINGS = () -> Rating.getAll();
	private static volatile Supplier<List<Keyword>> KEYWORDS = () -> Keyword.getAll();
	private static volatile Supplier<List<Credit>> CREDITS = () -> getAllFromMongo();
	// Placeholder for future improvement
	// Solution: cache them:
	private static volatile Supplier<List<Movie>> MOVIES = new CachedSupplier(() -> Movie.getAll());
	// private static volatile Supplier<List<Rating>> RATINGS = new CachedSupplier(() -> Rating.getAll());
	// private static volatile Supplier<List<Keyword>> KEYWORDS = new CachedSupplier(() -> Keyword.getAll());
	// // private static volatile Supplier<List<Credit>> CREDITS = new CachedSupplier(() -> Credit.getAll());
	// 	private static volatile Supplier<List<Credit>> CREDITS = new CachedSupplier(() -> getAllFromMongo());

	static {
		GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
	}

	public static void main(String[] args) {
		port(8081);
		get("/", Server::randomMovieEndpoint);
		get("/movies", Server::moviesEndpoint);
		get("/credits", Server::creditsEndpoint);
		get("/ratings", Server::ratingsEndpoint);
		get("/sleep", Server::sleepEndpoint);
		get("/mongo", Server::mongoEndpoint);
		get("/seedmongo", Server::seedMongo);

		// Warm up caches
		MOVIES.get();
		CREDITS.get();

		exception(Exception.class, (exception, request, response) -> {
			System.err.println(exception.getMessage());
			exception.printStackTrace();
		});
	}

	@Trace(operationName = "http.req", resourceName = "/movies")
	private static Object moviesEndpoint(Request req, Response res) {
		var movies = MOVIES.get().stream();
		movies = sortByDescReleaseDate(movies);
		var query = req.queryParamOrDefault("q", req.queryParams("query"));
		if (query != null) {
			// Problem: We are not compiling the pattern and there's a more efficient way of ignoring cases.
			movies = movies.filter(m -> m.title != null && Pattern.matches(".*" + query.toUpperCase() + ".*", m.title.toUpperCase()));
			// Solution:
			// var p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
			// movies = movies.filter(m -> m.title != null && p.matcher(m.title).find());
		}
		return replyJSON(res, movies);
	}

	private static Object randomMovieEndpoint(Request req, Response res) {
		var randomMovie = MOVIES.get().get(new Random().nextInt(MOVIES.get().size()));
		return replyJSON(res, randomMovie);
	}

	private static Object sleepEndpoint(Request req, Response res) {
		try {
			java.util.concurrent.TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) { }
		return replyJSON(res, "This is a slow endpoint");
	}

	private static class MovieWithCredits {

		public final Movie movie;
		public final List<Credit> credits;

		public MovieWithCredits(Movie movie, List<Credit> credits) {
			this.movie = movie;
			this.credits = credits;
		}
	}

	// $> time curl 'localhost:8081/credits?q=the' 1>/dev/null
	private static Object creditsEndpoint(Request req, Response res) {
		var movies = MOVIES.get().stream();
		// movies = sortByDescReleaseDate(movies);

		var query = req.queryParamOrDefault("q", req.queryParams("query"));
		if (query != null) {
			// Compile the pattern and more efficient way of ignoring cases.
			var p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
			movies = movies.filter(m -> m.title != null && p.matcher(m.title).find());
		}

		// Problem: We are doing a O(n^2) search
		var moviesWithCredits = movies.map(movie -> new MovieWithCredits(movie, creditsForMovie(movie)));
		// Solution: Use a map with O(1) access time, reducing the overall complexity to O(n)
		// var credits = CREDITS_BY_MOVIEID.get();
		// var moviesWithCredits = movies.map(m -> new MovieWithCredits(m, credits.get(m.id))).collect(Collectors.toList());

		return replyJSON(res, moviesWithCredits);
	}

	private static List<Credit> creditsForMovie(Movie movie) {
		return CREDITS.get().stream().filter(c -> c.id.equals(movie.id)).collect(Collectors.toList());
		// return CREDITS_BY_MOVIEID.get().get(movie.id);
	}

	// private static volatile Supplier<Map<Integer, List<Credit>>> CREDITS_BY_MOVIEID = () -> CREDITS.get().stream().collect(Collectors.groupingBy(c -> c.id));
	// private static volatile Supplier<Map<Integer, List<Credit>>> CREDITS_BY_MOVIEID = new CachedSupplier(() -> CREDITS.get().stream().collect(Collectors.groupingBy(c -> c.id)));

	private static class MovieWithRatings {

		public final Movie movie;
		public final List<Rating> ratings;

		public MovieWithRatings(Movie movie, List<Rating> ratings) {
			this.movie = movie;
			this.ratings = ratings;
		}
	}

	private static Object mongoEndpoint(Request req, Response res) {
		MongoClient mongoClient = MongoClients.create();
		var db = mongoClient.getDatabase("moviesDB");
		var collection = db.getCollection("credits");

		var alldocs = new java.util.ArrayList();

		collection.find().forEach(doc -> alldocs.add(doc));

		return replyJSON(res, alldocs.size());
	}

	private static Object seedMongo(Request req, Response res) {
		MongoClient mongoClient = MongoClients.create();
		var db = mongoClient.getDatabase("moviesDB");
		var collection = db.getCollection("credits");

		var allcredits = Parser.parse(Path.of("credits.json"), Document::new);

		System.out.println("Parsed " + allcredits.size());

		collection.insertMany(allcredits);

		return replyJSON(res, "OK");
	}

	private static List<Credit> getAllFromMongo() {
		var mongoClient = MongoClients.create();
		var moviesDatabase = mongoClient.getDatabase("moviesDB");
		var creditsCollection = moviesDatabase.getCollection("credits");

		return java.util.stream.StreamSupport.stream(creditsCollection.find().batchSize(50_000).map(Credit::new).spliterator(), false).collect(Collectors.toList());
	}

	// $> time curl 'localhost:8081/ratings?q=world' 1>/dev/null
	private static Object ratingsEndpoint(Request req, Response res) {
		var movies = MOVIES.get().stream();
		movies = sortByDescReleaseDate(movies);

		var query = req.queryParamOrDefault("q", req.queryParams("query"));
		if (query != null) {
			// Compile the pattern and more efficient way of ignoring cases.
			var p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
			movies = movies.filter(m -> m.title != null && p.matcher(m.title).find());
		}

		var ratings = RATINGS_BY_MOVIEID.get();
		var moviesWithRatings = movies.map(m -> new MovieWithRatings(m, ratings.get(m.id))).collect(Collectors.toList());

		return replyJSON(res, moviesWithRatings);
	}

	// Problem: we are rebuilding that every time
	private static volatile Supplier<Map<Integer, List<Rating>>> RATINGS_BY_MOVIEID = () -> RATINGS.get().stream().collect(Collectors.groupingBy(r -> r.movie_id));
	// Solution: cache it
	// private static volatile Supplier<Map<Integer, List<Rating>>> RATINGS_BY_MOVIEID = new CachedSupplier(() -> RATINGS.get().stream().collect(Collectors.groupingBy(r -> r.movie_id)));

	private static Stream<Movie> sortByDescReleaseDate(Stream<Movie> movies) {
		return movies.sorted(Comparator.comparing((Movie m) -> {
			// Problem: We are parsing a datetime for each item to be sorted.
			// Example Solution:
			//   Since date is in isoformat (yyyy-mm-dd) already, that one sorts nicely with normal string sorting
			//   `return m.release_date`
			try {
				return LocalDate.parse(m.release_date);
			} catch (Exception e) {
				return LocalDate.MIN;
			}
		}).reversed());
	}

	private static Object replyJSON(Response res, Stream<?> data) {
		return replyJSON(res, data.collect(Collectors.toList()));
	}

	private static Object replyJSON(Response res, Object data) {
		res.type("application/json");
		return GSON.toJson(data);
	}
}
