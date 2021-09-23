package movies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Parser {

	private static final JSONParser parser = new JSONParser();

	public static <T> List<T> parse(Path path, Function<JSONObject, T> consumer) {
		return parse(path, o -> true, consumer);
	}

	public static <T> List<T> parse(Path path, Function<JSONObject, Boolean> filter, Function<JSONObject, T> consumer) {
		List<T> objs = new ArrayList<>();

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				JSONObject obj = parse(line);
				if (!filter.apply(obj)) {
					continue;
				}

				try {
					objs.add(consumer.apply(obj));
				} catch (Throwable t) {
					// ignore
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return objs;
	}

	public static JSONObject parse(String data) {
		try {
			return (JSONObject)parser.parse(data);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}