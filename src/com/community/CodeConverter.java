package com.community;

import static com.community.Constants.VM_M_ARRAY_SIZE;
import static com.community.Primitives.NODE_TYPE.*;

/******************************************************************************
 * Copyright © 2017 The XEL Core Developers.                                  *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * XEL software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/
public class CodeConverter {

    private static class lrcast{
        public String lcast = null;
        public lrcast() {
        }
        public String rcast = null;
    }
    private static class lrstr{
        public String lstr = null;
        public lrstr() {
        }
        public String rstr = null;
    }

    // Hard Coded Tabs...Could Make This Dynamic
    private static String[] tab = { "", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t" };

    public static boolean strcmp(String a, String b){
        if(a.equalsIgnoreCase(b))
            return false;
        return true;
    }

    public static void convert_verify(Primitives.STATE state) throws Exceptions.SyntaxErrorException {
        for (int i = state.ast_func_idx; i < state.stack_exp.size(); i++) {
            convert_function(state, state.stack_exp.get(i));
        }
    }

    private static void get_node_inputs(Primitives.STATE state, Primitives.AST node, lrstr mylrstr) throws Exceptions.SyntaxErrorException {

        // Get Left & Right Values From Code Stack
        switch (node.type) {
            case NODE_VAR_EXP:
            case NODE_INCREMENT_R:
            case NODE_INCREMENT_L:
            case NODE_DECREMENT_R:
            case NODE_DECREMENT_L:
            case NODE_NOT:
            case NODE_COMPL:
            case NODE_NEG:
            case NODE_IF:
            case NODE_REPEAT:
            case NODE_VERIFY_BTY:
            case NODE_PARAM:
            case NODE_SIN:
            case NODE_COS:
            case NODE_TAN:
            case NODE_SINH:
            case NODE_COSH:
            case NODE_TANH:
            case NODE_ASIN:
            case NODE_ACOS:
            case NODE_ATAN:
            case NODE_EXPNT:
            case NODE_LOG:
            case NODE_LOG10:
            case NODE_SQRT:
            case NODE_CEIL:
            case NODE_FLOOR:
            case NODE_ABS:
            case NODE_FABS:
                try {
                    mylrstr.lstr = state.stack_code.pop();
                }catch (Exception e)
                {
                    throw new Exceptions.SyntaxErrorException("Compiler Error: Corupted code stack at Line: " +
                            node.line_num);
                }
                break;
            case NODE_ASSIGN:
            case NODE_ADD_ASSIGN:
            case NODE_SUB_ASSIGN:
            case NODE_MUL_ASSIGN:
            case NODE_DIV_ASSIGN:
            case NODE_MOD_ASSIGN:
            case NODE_LSHFT_ASSIGN:
            case NODE_RSHFT_ASSIGN:
            case NODE_AND_ASSIGN:
            case NODE_XOR_ASSIGN:
            case NODE_OR_ASSIGN:
            case NODE_ADD:
            case NODE_SUB:
            case NODE_MUL:
            case NODE_DIV:
            case NODE_MOD:
            case NODE_LSHIFT:
            case NODE_LROT:
            case NODE_RSHIFT:
            case NODE_RROT:
            case NODE_AND:
            case NODE_OR:
            case NODE_BITWISE_AND:
            case NODE_BITWISE_XOR:
            case NODE_BITWISE_OR:
            case NODE_EQ:
            case NODE_NE:
            case NODE_GT:
            case NODE_LT:
            case NODE_GE:
            case NODE_LE:
            case NODE_CONDITIONAL:
            case NODE_POW:
            case NODE_ATAN2:
            case NODE_FMOD:
            case NODE_GCD:
                try {
                    mylrstr.rstr = state.stack_code.pop();
                    mylrstr.lstr = state.stack_code.pop();
                }catch (Exception e)
                {
                    throw new Exceptions.SyntaxErrorException("Compiler Error: Corupted code stack at Line: " +
                            node.line_num);
                }
                break;
            case NODE_VERIFY_POW:
                try {
                    String[] pop = new String[4];
                    pop[0] = state.stack_code.pop();
                    pop[1] = state.stack_code.pop();
                    pop[2] = state.stack_code.pop();
                    pop[3] = state.stack_code.pop();
                    mylrstr.lstr = String.format("%s,%s,%s,%s",pop[3],pop[2],pop[1],pop[0]);
                }catch (Exception e)
                {
                    throw new Exceptions.SyntaxErrorException("Compiler Error: Corupted code stack at Line: " +
                            node.line_num);
                }
            case NODE_ELSE:
            case NODE_BLOCK:
            case NODE_COND_ELSE:
            case NODE_BREAK:
            case NODE_CONTINUE:
                break;
            default:
                break;
        }
    }

    private static void convert_node(Primitives.STATE state, Primitives.AST node) throws Exceptions.SyntaxErrorException {
        String op = null;
        lrcast mylrcast = new lrcast();
        lrstr mylrstr = new lrstr();
        String str = null;
        String tmp = null;
        boolean var_exp_flg = false;

        if (node==null)
            throw new Exceptions.SyntaxErrorException("Unable to convert NULL node.");

        // Get Left & Right Values From Code Stack
       get_node_inputs(state, node, mylrstr);

        switch (node.type) {
            case NODE_FUNCTION:
                str = String.format("function %s() {\n", node.svalue);
                break;
            case NODE_CALL_FUNCTION:
                if (!strcmp(node.svalue, "verify"))
                    str = String.format("%s(verify_pow, target)", node.svalue);
                else
                    str = String.format("%s()", node.svalue);
                break;
            case NODE_VERIFY_BTY:
                str = String.format("bounty_found = (%s != 0 ? 1 : 0)", mylrstr.lstr);
                break;
            case NODE_VERIFY_POW:
                str = String.format("if (verify_pow == 1)\n\t\tpow_found = ExposedToRhino.check_pow(%s, m, target);" +
                        "\n\telse\n\t\t{ pow_found = 0; ExposedToRhino.check_pow(%s, m, target); }", mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_CONSTANT:
                
                switch (node.data_type) {
                    case DT_INT:
                    case DT_LONG:
                        str = String.format("%d", node.ivalue);
                        break;
                    case DT_UINT:
                    case DT_ULONG:
                        str = String.format("%d", node.uvalue);
                        break;
                    case DT_FLOAT:
                    case DT_DOUBLE:
                        str = String.format("%f", node.fvalue);
                        break;
                    default:
                        throw new Exceptions.SyntaxErrorException("Compiler Error: Invalid constant at Line: "+ node.line_num);
                }
                break;
            case NODE_VAR_CONST:
                
                switch (node.data_type) {
                    case DT_INT:
                        str = String.format("i[%d]", ((node.uvalue >= state.ast_vm_ints) ? 0 : node.uvalue));
                        break;
                    case DT_UINT:
                        if (node.is_vm_mem)
                            str = String.format("m[%d]", ((node.uvalue >= state.ast_vm_uints) ? 0 : node.uvalue));
                        else if (node.is_vm_storage)
                            str = String.format("s[%d]", ((node.uvalue >= state.ast_vm_uints) ? 0 : node.uvalue));
                        else
                            str = String.format("u[%d]", ((node.uvalue >= state.ast_vm_uints) ? 0 : node.uvalue));
                        break;
                    case DT_LONG:
                        str = String.format("l[%d]", ((node.uvalue >= state.ast_vm_longs) ? 0 : node.uvalue));
                        break;
                    case DT_ULONG:
                        str = String.format("ul[%d]", ((node.uvalue >= state.ast_vm_ulongs) ? 0 : node.uvalue));
                        break;
                    case DT_FLOAT:
                        str = String.format("f[%d]", ((node.uvalue >= state.ast_vm_floats) ? 0 : node.uvalue));
                        break;
                    case DT_DOUBLE:
                        str = String.format("d[%d]", ((node.uvalue >= state.ast_vm_doubles) ? 0 : node.uvalue));
                        break;
                    default:
                        throw new Exceptions.SyntaxErrorException("Compiler Error: Invalid variable at Line: " + node
                                .line_num);
                }
                break;
            case NODE_VAR_EXP:
                if (node.parent.end_stmnt && (node == node.parent.left))
                    var_exp_flg = true;

                

                switch (node.data_type) {
                    case DT_INT:
                        if (var_exp_flg)
                            str = String.format("if((%s) < %d)\n\t%si[%s]", mylrstr.lstr, state.ast_vm_ints, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                        else
                            str = String.format("i[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_ints, mylrstr.lstr);
                        break;
                    case DT_UINT:
                        if (node.is_vm_mem) {
                            if (var_exp_flg)
                                str = String.format("if((%s) < %d)\n\t%sm[%s]", mylrstr.lstr, VM_M_ARRAY_SIZE, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                            else
                                str = String.format("m[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, VM_M_ARRAY_SIZE, mylrstr.lstr);
                        }
                        else if (node.is_vm_storage) {
                            if (var_exp_flg)
                                str = String.format("if((%s) < %d)\n\t%ss[%s]", mylrstr.lstr, state.ast_submit_sz, tab[state
                                        .tabs], mylrstr.lstr);
                            else
                                str = String.format("s[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_submit_sz, mylrstr.lstr);
                        }
                        else {
                            if (var_exp_flg)
                                str = String.format("if((%s) < %d)\n\t%su[%s]", mylrstr.lstr, state.ast_vm_uints, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                            else
                                str = String.format("u[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_uints, mylrstr.lstr);
                        }
                        break;
                    case DT_LONG:
                        if (var_exp_flg)
                            str = String.format("if((%s) < %d)\n\t%sl[%s]", mylrstr.lstr, state.ast_vm_longs, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                        else
                            str = String.format("l[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_longs, mylrstr.lstr);
                        break;
                    case DT_ULONG:
                        if (var_exp_flg)
                            str = String.format("if((%s) < %d)\n\t%sul[%s]", mylrstr.lstr, state.ast_vm_ulongs, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                        else
                            str = String.format("ul[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_ulongs, mylrstr.lstr);
                        break;
                    case DT_FLOAT:
                        if (var_exp_flg)
                            str = String.format("if((%s) < %d)\n\t%sf[%s]", mylrstr.lstr, state.ast_vm_floats, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                        else
                            str = String.format("f[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_floats, mylrstr.lstr);
                        break;
                    case DT_DOUBLE:
                        if (var_exp_flg)
                            str = String.format("if((%s) < %d)\n\t%sd[%s]", mylrstr.lstr, state.ast_vm_doubles, tab[Math.max(state.tabs,0)], mylrstr.lstr);
                        else
                            str = String.format("d[(((%s) < %d) ? %s : 0)]", mylrstr.lstr, state.ast_vm_doubles, mylrstr.lstr);
                        break;
                    default:
                        throw new Exceptions.SyntaxErrorException("Compiler Error: Invalid variable at Line: " + node
                                .line_num);
                }
                break;
            case NODE_IF:
                if (state.tabs < 1) state.tabs = 1;
                // Always Wrap "IF" In Brackets
                str = String.format("%sif (%s) {\n", tab[Math.max(state.tabs - 1,0)], mylrstr.lstr);
                break;
            case NODE_ELSE:
                if (state.tabs < 1) state.tabs = 1;

                // Check If "IF" Has Closing Bracket - If Not, Add Closing Bracket

                if(node.left != null && node.left.type != NODE_BLOCK)
                    str = String.format("%s}\n%selse {\n", tab[Math.max(state.tabs - 1,0)], tab[Math.max(state.tabs - 1,0)]);
                else
                    str = String.format("%selse {\n", tab[Math.max(state.tabs - 1,0)]);

                break;
            case NODE_REPEAT:
                
                if (state.tabs < 1) state.tabs = 1; // todo: not, i chaged u[%lld] t u[%d] ... check for problems please
                str = String.format("%svar loop%d = 0;\n%sfor (loop%d = 0; loop%d < (%s); loop%d++) {\n%s\tif (loop%d >= %d) break;\n%s\tu[%d] = loop%d;\n", tab[Math.max(state.tabs - 1,0)], node.token_num, tab[Math.max(state.tabs - 1,0)], node.token_num, node.token_num, mylrstr.lstr, node.token_num, tab[Math.max(state.tabs - 1,0)], node.token_num, node.ivalue, tab[Math.max(state.tabs - 1,0)], node.uvalue, node.token_num);
                break;
            case NODE_BLOCK:
                
                if (node.parent.type == NODE_FUNCTION) {
                    str = String.format("}\n");
                }
                else if(node.parent.type == NODE_REPEAT){ // Todo: this might be redundant
                    str = String.format("%s}\n", tab[Math.max(state.tabs - 1,0)]);
                }
                else if ((node.parent.type == NODE_ELSE) && (node == node.parent.right)) {
                    str = "";
                }
                else
                    str = String.format("%s}\n", tab[Math.max(state.tabs - 1,0)]);
                break;
            case NODE_BREAK:
                
                str = String.format("break");
                break;
            case NODE_CONTINUE:
                
                str = String.format("continue");
                break;

            case NODE_CONDITIONAL:
                try {
                    tmp = state.stack_code.pop();
                }catch(Exception e){
                    throw new Exceptions.SyntaxErrorException("Compiler Error: Corupted code stack at Line: " + node
                            .line_num);
                }
                
                str = String.format("((%s) ? (%s) : (%s))", tmp, mylrstr.lstr, mylrstr.rstr);
                
                break;

            case NODE_COND_ELSE:
                return;

            case NODE_ADD:
            case NODE_SUB:
            case NODE_MUL:
            case NODE_EQ:
            case NODE_NE:
            case NODE_GT:
            case NODE_LT:
            case NODE_GE:
            case NODE_LE:
            case NODE_AND:
            case NODE_OR:
            case NODE_BITWISE_AND:
            case NODE_BITWISE_XOR:
            case NODE_BITWISE_OR:
            case NODE_LSHIFT:
            case NODE_RSHIFT:
                switch (node.type) {
                    case NODE_ADD:			op = String.format("%s", "+");		break;
                    case NODE_SUB:			op = String.format("%s", "-");		break;
                    case NODE_MUL:			op = String.format("%s", "*");		break;
                    case NODE_EQ:			op = String.format("%s", "==");	break;
                    case NODE_NE:			op = String.format("%s", "!=");	break;
                    case NODE_GT:			op = String.format("%s", ">");		break;
                    case NODE_LT:			op = String.format("%s", "<");		break;
                    case NODE_GE:			op = String.format("%s", ">=");	break;
                    case NODE_LE:			op = String.format("%s", "<=");	break;
                    case NODE_AND:			op = String.format("%s", "&&");	break;
                    case NODE_OR:			op = String.format("%s", "||");	break;
                    case NODE_BITWISE_AND:	op = String.format("%s", "&");		break;
                    case NODE_BITWISE_XOR:	op = String.format("%s", "^");		break;
                    case NODE_BITWISE_OR:	op = String.format("%s", "|");		break;
                    case NODE_LSHIFT:		op = String.format("%s", "<<");	break;
                    case NODE_RSHIFT:		op = String.format("%s", ">>");	break;
                }
                
                //get_cast(mylrcast, node.left.data_type, node.right.data_type, false); // TODO FIX UINT, ALSO THIS IS GONE IN COMMIT fa0611b89 ... why?
                /*if (mylrcast.lcast != null)
                    str = String.format("(%s)(%s) %s (%s)", mylrcast.lcast, mylrstr.lstr, op, mylrstr.rstr);
                else if (mylrcast.rcast != null)
                    str = String.format("(%s) %s (%s)(%s)", mylrstr.lstr, op, mylrcast.rcast, mylrstr.rstr);
                else*/
                str = String.format("(%s) %s (%s)", mylrstr.lstr, op, mylrstr.rstr);
                break;

            case NODE_DIV:
            case NODE_MOD:
                switch (node.type) {
                    case NODE_DIV:	op = String.format("%s", "/"); break;
                    case NODE_MOD:	op = String.format("%s", "%"); break;
                }
                
                //get_cast(mylrcast, node.left.data_type, node.right.data_type, true); // TODO FIX UINT
                /*if (mylrcast.rcast!=null)
                    str = String.format("(((%s) != 0) ? (%s) %s (%s)(%s) : 0)", mylrstr.rstr, mylrstr.lstr, op, mylrcast.rcast, mylrstr.rstr);
                else*/
                str = String.format("(((%s) != 0) ? (%s) %s (%s) : 0)", mylrstr.rstr, mylrstr.lstr, op, mylrstr.rstr);
                break;

            case NODE_ASSIGN:
            case NODE_ADD_ASSIGN:
            case NODE_SUB_ASSIGN:
            case NODE_MUL_ASSIGN:
            case NODE_LSHFT_ASSIGN:
            case NODE_RSHFT_ASSIGN:
            case NODE_AND_ASSIGN:
            case NODE_XOR_ASSIGN:
            case NODE_OR_ASSIGN:
                switch (node.type) {
                    case NODE_ASSIGN:		op = String.format("%s", "=");		break;
                    case NODE_ADD_ASSIGN:	op = String.format("%s", "+=");	break;
                    case NODE_SUB_ASSIGN:	op = String.format("%s", "-=");	break;
                    case NODE_MUL_ASSIGN:	op = String.format("%s", "*=");	break;
                    case NODE_LSHFT_ASSIGN:	op = String.format("%s", "<<=");	break;
                    case NODE_RSHFT_ASSIGN:	op = String.format("%s", ">>=");	break;
                    case NODE_AND_ASSIGN:	op = String.format("%s", "&=");	break;
                    case NODE_XOR_ASSIGN:	op = String.format("%s", "^=");	break;
                    case NODE_OR_ASSIGN:	op = String.format("%s", "|=");	break;
                }
                
                //get_cast(mylrcast, node.left.data_type, node.right.data_type, true); // TODO FIX UINT
                /*if (mylrcast.rcast!=null)
                    str = String.format("%s %s (%s)(%s)", mylrstr.lstr, op, mylrcast.rcast, mylrstr.rstr);
                else*/
                str = String.format("%s %s %s", mylrstr.lstr, op, mylrstr.rstr);
                break;

            case NODE_DIV_ASSIGN:
            case NODE_MOD_ASSIGN:
                switch (node.type) {
                    case NODE_DIV_ASSIGN:	op = String.format("%s", "/");	break;
                    case NODE_MOD_ASSIGN:	op = String.format("%s", "%");	break;
                }
                
                // get_cast(mylrcast, node.left.data_type, node.right.data_type, true); // TODO FIX UINT
                /*if (mylrcast.rcast != null)
                    str = String.format("%s = (((%s) != 0) ? (%s) %s (%s)(%s) : 0)", mylrstr.lstr, mylrstr.rstr, mylrstr.lstr, op, mylrcast.rcast,
                            mylrstr.rstr);
                else*/
                str = String.format("%s = (((%s) != 0) ? (%s) %s (%s) : 0)", mylrstr.lstr, mylrstr.rstr, mylrstr.lstr, op, mylrstr.rstr);
                break;

            case NODE_INCREMENT_R:
                
                str = String.format("++%s", mylrstr.lstr);
                break;

            case NODE_INCREMENT_L:
                
                str = String.format("%s++", mylrstr.lstr);
                break;

            case NODE_DECREMENT_R:
                
                str = String.format("--%s", mylrstr.lstr);
                break;

            case NODE_DECREMENT_L:
                
                str = String.format("%s--", mylrstr.lstr);
                break;

            case NODE_NOT:
                
                str = String.format("!(%s)", mylrstr.lstr);
                break;

            case NODE_COMPL:
                
                str = String.format("~(%s)", mylrstr.lstr);
                break;

            case NODE_NEG:
                
                str = String.format("-(%s)", mylrstr.lstr);
                break;

            case NODE_LROT:
                
                if (node.is_64bit)
                    str = String.format("rotl64(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                else
                    str = String.format("rotl32(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                break;
            case NODE_RROT:
                
                if (node.is_64bit)
                    str = String.format("rotr64(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                else
                    str = String.format("rotr32(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                break;

            case NODE_PARAM:
                
                str = String.format("%s", mylrstr.lstr);
                break;
            case NODE_ABS:
                
                str = String.format("Math.abs(%s)", mylrstr.lstr);
                break;
            case NODE_POW:
                
                str = String.format("Math.pow(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                break;
            case NODE_SIN:
                
                str = String.format("Math.sin(%s)", mylrstr.lstr);
                break;
            case NODE_COS:
                
                str = String.format("Math.cos(%s)", mylrstr.lstr);
                break;
            case NODE_TAN:
                
                str = String.format("Math.tan(%s)", mylrstr.lstr);
                break;
            case NODE_SINH:
                
                str = String.format("(((%s >= -1.0) && (%s <= 1.0)) ? Math.sinh( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_COSH:
                
                str = String.format("(((%s >= -1.0) && (%s <= 1.0)) ? Math.cosh( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_TANH:
                
                str = String.format("Math.tanh(%s)", mylrstr.lstr);
                break;
            case NODE_ASIN:
                
                str = String.format("(((%s >= -1.0) && (%s <= 1.0)) ? Math.asin( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_ACOS:
                
                str = String.format("(((%s >= -1.0) && (%s <= 1.0)) ? Math.acos( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_ATAN:
                
                str = String.format("Math.atan(%s)", mylrstr.lstr);
                break;
            case NODE_ATAN2:
                
                str = String.format("((%s != 0) ? Math.atan2(%s, %s) : 0.0)", mylrstr.rstr, mylrstr.lstr, mylrstr.rstr);
                break;
            case NODE_EXPNT:
                
                str = String.format("((((%s) >= -708.0) && ((%s) <= 709.0)) ? Math.exp( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_LOG:
                
                str = String.format("((%s > 0) ? Math.log( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_LOG10:
                
                str = String.format("((%s > 0) ? Math.log10( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_SQRT:
                
                str = String.format("((%s > 0) ? Math.sqrt( %s ) : 0.0)", mylrstr.lstr, mylrstr.lstr);
                break;
            case NODE_CEIL:
                
                str = String.format("Math.ceil(%s)", mylrstr.lstr);
                break;
            case NODE_FLOOR:
                
                str = String.format("Math.floor(%s)", mylrstr.lstr);
                break;
            case NODE_FABS:
                
                str = String.format("Math.abs(%s)", mylrstr.lstr);
                break;
            case NODE_FMOD:
                //TODO: FMOD is not supported, mod is wrong here! FIX ASAP
                str = String.format("((%s != 0) ? (%s %% %s) : 0.0)", mylrstr.rstr, mylrstr.lstr, mylrstr.rstr);
                break;
            case NODE_GCD:
                
                str = String.format("gcd(%s, %s)", mylrstr.lstr, mylrstr.rstr);
                break;
            default:
                throw new Exceptions.SyntaxErrorException("Compiler Error: Unknown expression at Line: " + node
                        .line_num);
        }

        mylrstr.lstr = null;
        mylrstr.rstr = null;

        // Terminate Statements
        if (node.end_stmnt && (node.type != NODE_IF) && (node.type != NODE_ELSE) && (node.type != NODE_REPEAT) && (node.type != NODE_BLOCK) && (node.type != NODE_FUNCTION)) {
            tmp = String.format("%s%s;\n", tab[Math.max(state.tabs,0)], str);
            state.stack_code.push(tmp);

            /* Removed this in the context of the bracket fix
            // Add Closing Bracket To IF / ELSE That Don't Have Them
            if (node.parent != null && node.parent.right != null && (node.parent.right.type != NODE_BLOCK) && ((node.parent.type == NODE_IF) || (node.parent.type == NODE_ELSE))) {
                tmp = String.format("%s}\n", tab[Math.max(state.tabs,0)], str);
                state.stack_code.push(tmp);
            }
            */
        }
        else {
            state.stack_code.push(str);
        }
    }
    
    public static void convert_function(Primitives.STATE state, Primitives.AST root) throws Exceptions.SyntaxErrorException {
        boolean downward = true;
        Primitives.AST ast_ptr = null;

        if (root == null)
            throw new Exceptions.SyntaxErrorException("Unable to convert NULL object.");

        ast_ptr = root;
        state.tabs++;

        while (ast_ptr != null) {

            // Navigate Down The Tree
            if (downward) {

                // Navigate To Lowest Left Node
                while (ast_ptr.left != null) {
                    ast_ptr = ast_ptr.left;

                    // Indent Statements Below IF / REPEAT
                    if ((ast_ptr.type == NODE_IF) || (ast_ptr.type == NODE_REPEAT))
                        state.tabs++;
                }

                if (ast_ptr.type == NODE_FUNCTION)
                    convert_node(state, ast_ptr);

                // If There Is A Right Node, Switch To It
                if (ast_ptr.right != null) {
                    ast_ptr = ast_ptr.right;
                }
                // Otherwise, Convert Current Node & Navigate Back Up The Tree
                else {
                    convert_node(state, ast_ptr);
                    downward = false;
                }
            }

            // Navigate Back Up The Tree
            else {
                if (ast_ptr.parent == root)
                    break;

                // Check If We Need To Navigate Back Down A Right Branch
                if ((ast_ptr == ast_ptr.parent.left) && (ast_ptr.parent.right != null)) {
                    ast_ptr = ast_ptr.parent.right;

                    if ((ast_ptr.parent.type == NODE_IF) || (ast_ptr.parent.type == NODE_ELSE) || (ast_ptr.parent.type == NODE_REPEAT)) {
                        convert_node(state, ast_ptr.parent);

                        if ((ast_ptr.type == NODE_IF) || (ast_ptr.type == NODE_REPEAT))
                            state.tabs++;
                    }

                    downward = true;
                }
                else {
                    ast_ptr = ast_ptr.parent;
                    if ((ast_ptr.type == NODE_IF) || (ast_ptr.type == NODE_ELSE) || (ast_ptr.type == NODE_REPEAT)) {
                        if (state.tabs > 0) state.tabs--;

                        // Add Closing Bracket If Needed
                        if ((ast_ptr.type != NODE_ELSE) && (ast_ptr.right != null && (ast_ptr.right.type != NODE_BLOCK))) {
                            String str = String.format("%s}\n", tab[Math.max(state.tabs,0)]);
                            state.stack_code.push(str);
                        }

                        // Indent Else Statements To Match Corresponding IF
                        if ((ast_ptr.type == NODE_ELSE)) {
                            state.tabs++;
                        }

                    } else if (ast_ptr.type == NODE_BLOCK) {
                        if ((ast_ptr.parent.type == NODE_IF) || (ast_ptr.parent.type == NODE_ELSE) || (ast_ptr.parent.type == NODE_REPEAT) || (ast_ptr.parent.type == NODE_FUNCTION)) {
                            convert_node(state, ast_ptr);
                        }
                    } else {
                        convert_node(state, ast_ptr);
                    }
                }
            }
        }
    }

}
