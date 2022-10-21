import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;
import java.util.function.Function;

public class MyThread extends Thread{
    private int x;
    private MyState state = MyState.STARTED;
    private Function<Integer, Optional<Optional<Integer>>> function;
    private PipedInputStream inputStream;
    private PipedOutputStream outputStream;

    public MyThread(Function<Integer, Optional<Optional<Integer>>> function, PipedInputStream inputStream, PipedOutputStream outputStream) {
        this.function = function;
        try {
            this.inputStream = new PipedInputStream(outputStream);
            this.outputStream = new PipedOutputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void setX() {
        try {
            this.x = inputStream.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MyState getMyState() {
        return state;
    }

    public enum MyState {
        STARTED,
        SOFT_FAIL,
        HARD_FAIL,
        FINISHED
    }

    @Override
    public void run() {
        try {
            Optional<Optional<Integer>> soft = Optional.empty();
            for (int i = 0; i < Manager.MAX_ATTEMPTS; ++i) {
                soft = function.apply(x);
                if (soft.isPresent()) {
                    break;
                }
            }
            if (soft.isPresent()) {
                Optional<Integer> hard = soft.get();
                if (hard.isPresent()) {
                    int result = hard.get();
                    state = MyState.FINISHED;
                    outputStream.write(result);
                } else {
                    state = MyState.HARD_FAIL;
                    System.exit(Integer.parseInt(Thread.currentThread().getName()));
                }
            } else {
                state = MyState.SOFT_FAIL;
                System.exit(Integer.parseInt(Thread.currentThread().getName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
