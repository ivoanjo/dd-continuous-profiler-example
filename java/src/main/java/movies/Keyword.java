package movies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.List;

import org.json.simple.JSONObject;

public final class Keyword {

	public final Integer id;
	public final String keywords;

	private Keyword(JSONObject data) {
		this.id = Integer.parseInt((String)data.get("id"));
		this.keywords = (String)data.get("keywords");
	}

	private static List<Keyword> parse() {
		return Parser.parse(Path.of("keywords.json"), Keyword::new);
	}

	public static List<Keyword> getAll() {
		return parse();
	}
}