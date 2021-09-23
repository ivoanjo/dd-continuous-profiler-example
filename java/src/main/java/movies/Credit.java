package movies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.List;

import org.json.simple.JSONObject;

public final class Credit {

	public final Integer id;
	public final String crew;
	public final String cast;

	private Credit(JSONObject data) {
		this.id = Integer.parseInt((String)data.get("id"));
		this.crew = (String)data.get("crew");
		this.cast = (String)data.get("cast");
	}

	private static List<Credit> parse() {
		return Parser.parse(Path.of("credits.json"), Credit::new);
	}

	public static List<Credit> getAll() {
		return parse();
	}
}