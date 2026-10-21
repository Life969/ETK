package ProjectForJob.example.Job.exception.util;

public class CalculatorException extends IllegalArgumentException {
    private final CalculatorErrorCode errorCode;
    public CalculatorException(CalculatorErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    CalculatorException(CalculatorErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public CalculatorErrorCode getCode() {return errorCode;}
}

