package org.epiviz.minifier;

import com.google.javascript.jscomp.CommandLineRunner;

public class EpivizClosureCommandLineRunner extends CommandLineRunner {
    public EpivizClosureCommandLineRunner(String[] args) {
        super(args);
        // TODO Auto-generated constructor stub
    }

    /**
     * Runs the Compiler and calls System.exit() with the exit status of the
     * compiler.
     */
    public void epivizRun() {
      int result = 0;
      int runs = 1;
      try {
        for (int i = 0; i < runs && result == 0; i++) {
          result = this.doRun();
        }
      } catch (Exception e) {
        System.err.println(e.getMessage());
        result = -1;
      } catch (Throwable t) {
        t.printStackTrace();
        result = -2;
      }
    }
}
