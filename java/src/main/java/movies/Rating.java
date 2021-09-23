package movies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.List;

import org.json.simple.JSONObject;

public final class Rating {

	public final Integer user_id;
	public final Integer movie_id;
	public final String rating;
	public final String timestamp;

	private Rating(JSONObject data) {
		this.user_id = Integer.parseInt((String)data.get("userId"));
		this.movie_id = Integer.parseInt((String)data.get("movieId"));
		this.rating = (String)data.get("rating");
		this.timestamp = (String)data.get("timestamp");
	}

	private static List<Rating> parse() {
		return Parser.parse(Path.of("ratings.json"), Rating::new);
	}

	public static List<Rating> getAll() {
		return parse();
	}
}