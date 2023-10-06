package io.github.pixee.maven.operator;

public class WrongDependencyTypeException extends RuntimeException {
    public WrongDependencyTypeException(String message) {
        super(message);
    }
}

