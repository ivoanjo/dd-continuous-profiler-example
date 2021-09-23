package movies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.List;

import org.json.simple.JSONObject;

public final class Movie {

	public final Integer id;
	public final String belongs_to_collection;
	public final String budget;
	public final String genres;
	public final String homepage;
	public final String imdb_id;
	public final String original_language;
	public final String original_title;
	public final String overview;
	public final String popularity;
	public final String poster_path;
	public final String production_companies;
	public final String production_countries;
	public final String release_date;
	public final String revenue;
	public final String runtime;
	public final String spoken_languages;
	public final String status;
	public final String tagline;
	public final String title;
	public final String video;
	public final String vote_average;
	public final String vote_count;

	private Movie(JSONObject data) {
		this.id = Integer.parseInt((String)data.get("id"));
		this.belongs_to_collection = (String)data.get("belongs_to_collection");
		this.budget = (String)data.get("budget");
		this.genres = (String)data.get("genres");
		this.homepage = (String)data.get("homepage");
		this.imdb_id = (String)data.get("imdb_id");
		this.original_language = (String)data.get("original_language");
		this.original_title = (String)data.get("original_title");
		this.overview = (String)data.get("overview");
		this.popularity = (String)data.get("popularity");
		this.poster_path = (String)data.get("poster_path");
		this.production_companies = (String)data.get("production_companies");
		this.production_countries = (String)data.get("production_countries");
		this.release_date = (String)data.get("release_date");
		this.revenue = (String)data.get("revenue");
		this.runtime = (String)data.get("runtime");
		this.spoken_languages = (String)data.get("spoken_languages");
		this.status = (String)data.get("status");
		this.tagline = (String)data.get("tagline");
		this.title = (String)data.get("title");
		this.video = (String)data.get("video");
		this.vote_average = (String)data.get("vote_average");
		this.vote_count = (String)data.get("vote_count");
	}

	private static List<Movie> parse() {
		return Parser.parse(Path.of("movies.json"), d -> !Boolean.parseBoolean((String)d.get("adult")), Movie::new);
	}

	public static List<Movie> getAll() {
		return parse();
	}
}