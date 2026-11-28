package it.unibo.mvc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Application entry point for DrawNumber game.
 */
public final class DrawNumberApp implements DrawNumberViewObserver {

    private static final String CONFIG_FILE = "config.yml";

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * Constructor.
     *
     * @param views
     *              the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view : views) {
            view.setObserver(this);
            view.start();
        }

        Configuration config;
        try {
            config = readConfiguration();
        } catch (IOException | NumberFormatException | NoSuchElementException e) {
            config = new Configuration.Builder().build();
            for (final DrawNumberView view : this.views) {
                view.displayError("impossibile leggere la configurazione: " + e.getMessage());
            }
        }
        this.model = new DrawNumberImpl(config.getMin(), config.getMax(), config.getAttempts());
    }

    private Configuration readConfiguration() throws IOException {

        final Configuration.Builder builder = new Configuration.Builder();
        final InputStream in = ClassLoader.getSystemResourceAsStream(CONFIG_FILE);
        if (in == null) {
            throw new FileNotFoundException("file non trovato" + CONFIG_FILE);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    final StringTokenizer st = new StringTokenizer(line, ":");
                    final String key = st.nextToken().trim();
                    final int value = Integer.parseInt(st.nextToken().trim());
                    switch (key) {
                        case "minimum" -> builder.withMin(value);
                        case "maximum" -> builder.withMax(value);
                        case "attemps" -> builder.withAttempts(value);
                        default -> throw new IOException("Chiave sconosciuta: " + key);
                    }
                }
                line = reader.readLine();
            }
        }
        return builder.build();
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view : views) {
                view.result(result);
            }
        } catch (final IllegalArgumentException e) {
            for (final DrawNumberView view : views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    @SuppressFBWarnings(value = "DM_EXIT", justification = "Acceptable for exercising purposes.")
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * Application entry point.
     *
     * @throws FileNotFoundException if the configuration file cannot be fetched
     */
    public static void main() throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl());
    }

}
