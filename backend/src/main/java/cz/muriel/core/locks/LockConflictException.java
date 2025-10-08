package cz.muriel.core.locks;

import lombok.Getter;

/**
 * Exception thrown when lock conflict occurs
 */
@Getter
public class LockConflictException extends RuntimeException {

  private final EditLock existingLock;

  public LockConflictException(String message, EditLock existingLock) {
    super(message);
    this.existingLock = existingLock;
  }
}
