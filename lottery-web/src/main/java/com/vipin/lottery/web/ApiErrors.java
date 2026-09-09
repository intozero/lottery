package com.vipin.lottery.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid request":e.getMessage()));}
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tooLarge(){return ResponseEntity.status(413).body(Map.of("error","Maximum file size is 3 MB"));}
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> unavailable(IOException e){return ResponseEntity.status(502).body(Map.of("error","Could not read the official source. Retry later. "+e.getMessage()));}
    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<?> interrupted(){Thread.currentThread().interrupt();return ResponseEntity.status(503).body(Map.of("error","The update was interrupted; retry later."));}
}
