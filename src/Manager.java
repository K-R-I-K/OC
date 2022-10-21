import os.lab1.compfuncs.advanced.IntOps;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

public class Manager {
    private int x;
    private final Thread cancellation;
    private final MyThread threadF;
    private final MyThread threadG;
    private final PipedInputStream inputStreamF;
    private final PipedInputStream inputStreamG;
    private final PipedOutputStream outputStreamF;
    private final PipedOutputStream outputStreamG;
    public static final int MAX_ATTEMPTS = 3;

    public Manager() {
        cancellation = new Thread(getCancellationRunnable());
        cancellation.setDaemon(true);

        Function<Integer, Optional<Optional<Integer>>> f = (x) -> {
            try {
                return IntOps.trialF(x);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        Function<Integer, Optional<Optional<Integer>>> g = (x) -> {
            try {
                return IntOps.trialG(x);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        inputStreamF = new PipedInputStream();
        inputStreamG = new PipedInputStream();
        outputStreamF = new PipedOutputStream();
        outputStreamG = new PipedOutputStream();

        threadF = new MyThread(f, inputStreamF, outputStreamF);
        threadG = new MyThread(g, inputStreamG, outputStreamG);
        threadF.setName("1");
        threadG.setName("2");
        threadF.setDaemon(true);
        threadG.setDaemon(true);

        Runtime current = Runtime.getRuntime();
        current.addShutdownHook(new Thread(getHookRunnable()));
    }

    private Runnable getHookRunnable() {
        return () -> {
            System.out.println("Computations finished:");
            switch (threadF.getMyState()) {
                case STARTED -> System.out.println("f not finished");
                case SOFT_FAIL -> System.out.println("f failed (soft fail)");
                case HARD_FAIL -> System.out.println("f failed (hard fail)");
                case FINISHED -> System.out.println("f finished");
            }
            switch (threadG.getMyState()) {
                case STARTED -> System.out.println("g not finished");
                case SOFT_FAIL -> System.out.println("g failed (soft fail)");
                case HARD_FAIL -> System.out.println("g failed (hard fail)");
                case FINISHED -> System.out.println("g finished");
            }
        };
    }

    private Runnable getCancellationRunnable(){
        return () -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                while (!scanner.hasNextLine()) {}
                String input = scanner.nextLine();
                if (input.equals("e")) {
                    System.exit(0);
                }
            }
        };
    }

    private void readFromConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter x:");
        String input = scanner.nextLine();
        try {
            x = Integer.parseInt(input);
        }catch (NumberFormatException e) {
            System.out.println("Invalid input");
        }
    }
    public void start(){
        System.out.println("For exit print 'e'");
        readFromConsole();
        try {
            outputStreamF.write(x);
            outputStreamG.write(x);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        threadF.setX();
        threadG.setX();

        cancellation.start();

        threadF.start();
        threadG.start();

        int resultF;
        int resultG;
        try {
            resultF = inputStreamF.read();
            resultG = inputStreamG.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(threadF.getMyState() == MyThread.MyState.FINISHED &&
                threadG.getMyState() == MyThread.MyState.FINISHED){
            System.out.println(resultF + resultG);
        }
    }
}
