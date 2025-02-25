package org.oyyj.userservice.exception;

public class SourceException extends Exception{
    public SourceException(){
        super("来源错误");
    }

    public SourceException(String message){
        super(message);
    }


}
