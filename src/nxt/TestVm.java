package nxt;


import com.community.*;
import com.community.Constants;
import nxt.computation.ComputationConstants;
import nxt.util.Convert;
import nxt.util.Logger;
import org.mozilla.javascript.NativeArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import static nxt.Nxt.NXT_DEFAULT_TESTVM_PROPERTIES;
import static nxt.Nxt.loadProperties;


public class TestVm {
    private static final Properties defaultProperties = new Properties();

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = defaultProperties.getProperty(name);
        if (value != null && !"".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static Boolean getBooleanProperty(String name) {
        String value = defaultProperties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static void main(String[] args) {
        loadProperties(defaultProperties, NXT_DEFAULT_TESTVM_PROPERTIES, true);
        String file = getStringProperty("nxt.test_file");
        boolean dumpTokens = getBooleanProperty("nxt.dump_tokens");
        boolean dumpAst = getBooleanProperty("nxt.dump_ast");
        boolean dumpCode = getBooleanProperty("nxt.dump_code");
        boolean exec = getBooleanProperty("nxt.execute_code");
        try {
            String content = new Scanner(new File(file)).useDelimiter("\\Z").next();
            TokenManager t = new TokenManager();
            t.build_token_list(content);

            if (dumpTokens) {
                Logger.logMessage("Dumping the token list now");
                System.out.flush();
                System.err.flush();
                t.dump_token_list();
            }

            ASTBuilder.parse_token_list(t.state);

            if (dumpAst) {
                Logger.logMessage("Dumping AST");
                System.out.flush();
                System.err.flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ASTBuilder.dump_vm_ast(t.state);
            }

            int wcet = WCETCalculator.calc_wcet(t.state);
            int verify_wcet = WCETCalculator.get_verify_wcet(t.state);

            Logger.logMessage("WCET: " + wcet);
            Logger.logMessage("Verify-WCET: " + verify_wcet);
            System.out.flush();
            System.err.flush();

            CodeConverter.convert_verify(t.state);

            String result = "";
            for (int i = 0; i < t.state.stack_code.size(); ++i) {
                result += t.state.stack_code.get(i);
            }

            // Add global variables and functions
            String pre_code = "var pow_found = 0;\nvar bounty_found = 0;\n";
            pre_code += "function rotr32(word, shift) {\n" +
                    "  return word << 32 - shift | word >>> shift;\n" +
                    "}\n";

            pre_code += "function rotl32(word, shift) {\n" +
                    "  return word << shift | word >>> 32 - shift;\n" +
                    "}\n";


            String code = pre_code + "\n" + result;
            if (dumpCode) {
                Logger.logMessage("Dumping generated source code");
                System.out.flush();
                System.err.flush();
                System.out.println(code);
            }

            if (exec) {
                Logger.logMessage("We will now execute the code");

                delight.rhinosandox.RhinoSandbox sandbox = delight.rhinosandox.RhinoSandboxes.create();
                sandbox.setInstructionLimit(com.community.Constants.INSTRUCTION_LIMIT);
                sandbox.setMaxDuration(Constants.SAFE_TIME_LIMIT);
                sandbox.allow(ExposedToRhino.class);

                // Add some dummy storae
                int[] s = new int[10000];
                sandbox.inject("s", s); // todo, add extra elements to S[] as coralreefer proposed

                int[] target = Convert.bigintToInts(ComputationConstants.MAXIMAL_WORK_TARGET, 4);
                sandbox.inject("target", target);
                sandbox.inject("verify_pow", false); // always go for the bounty check here

                // Inject temp arrays
                int[] m = fakeInts();


                int[] u = new int[10000];

                // now, fill the validator uints! (also called "data" in xelminer)
                    /*
                    leave this out for now
                    for (int i = 0; i < validator.length; ++i) {
                        u[validator_offset_index + i] = validator[i];
                    }*/

                // TO CHECK; WE GIVE I ARRAY AS M-VARIABLE TO JS, BUT IT DOES NOT CONTAIN M IN ANY CASE! PLEASE ELABORATE ON THIS, THIS MIGHT BE THE ERROR! PUT I IN M AND YOURE POSSIBLY DONE

                int[] i = new int[10000];
                // fill beginning of i (or better say m array) with deterministic stuff
                for (int pyx = 0; pyx < m.length; ++pyx)
                    i[pyx] = m[pyx];


                float[] f = new float[10000]; // TODO: Make dynamic
                double[] d = new double[10000];
                long[] l = new long[10000];
                long[] ul = new long[10000];

                sandbox.inject("u", u);
                sandbox.inject("m", m);
                sandbox.inject("i", i);
                sandbox.inject("l", l);
                sandbox.inject("ul", ul);
                sandbox.inject("f", f);
                sandbox.inject("d", d);


                // Add native java object for Rhino exposed POW functions
                sandbox.inject("ExposedToRhino", Executor.jsObj);

                code = code + " verify(); function res(){ return [pow_found, " +
                        "bounty_found]; } " +
                        "res();";

                org.mozilla.javascript.NativeArray array = (NativeArray) sandbox.eval("epl", code);
            }

        } catch (FileNotFoundException e) {
            Logger.logErrorMessage("File " + file + " not found.");
        } catch (Exception e) {
            Logger.logErrorMessage("The following syntax error has been found");
            System.out.flush();
            System.err.flush();
            e.printStackTrace();
        }
    }

    private static int[] fakeInts() {
        int[] ints = new int[12];
        return ints;
    }
}
