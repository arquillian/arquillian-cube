package org.arquillian.cube.kubernetes.impl.log;

import org.arquillian.cube.kubernetes.api.Logger;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

public class AnsiLogger implements Logger {

    @Override
    public void info(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg) {
        System.out.println( ansi().fg(YELLOW).a(msg).reset() );
    }

    @Override
    public void error(String msg) {
        System.out.println(ansi().fg(RED).a(msg).reset());
    }

    @Override
    public void status(String msg) {
        System.out.println(ansi().fg(GREEN).a(msg).reset());
    }

}
