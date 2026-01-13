import java.util.ArrayList;
import java.util.List;

public class RespParser {
    public static List<String> decode(String data) {
        List<String> args = new ArrayList<>();

        if (data == null || data.isEmpty()) return args;

        String[] lines = data.split("\r\n");

        if (lines[0].startsWith("*")) {

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("$")) {
                    continue;
                }
                args.add(line);
            }
        } else {

            String[] tokens = data.trim().split("\\s+");
            for (String token : tokens) {
                args.add(token);
            }
        }

        return args;
    }
}