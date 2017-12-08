package com.community;

import nxt.util.Convert;

import java.util.Stack;

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
public class EnigmaVM {

    static class EnigmaException extends Exception {
        // Parameterless Constructor
        public EnigmaException() {
        }

        // Constructor that accepts a message
        public EnigmaException(String message) {
            super(message);
        }
    }

    public static void stepProgram(EnigmaProgram prog) throws EnigmaException {

        byte[] value;
        byte[] key;
        int integerKey;

        if(prog.isStopped())
            return;

        EnigmaOpCode op = EnigmaOpCode.findOpCode(prog.getCurrentOperation());

        if (op == null) {
            throw new EnigmaException(String.format("Unknown OP-Code: %x", prog.getCurrentOperation()));
        }

        // Make sure stack is large enough
        if (prog.getStackSize() < op.getInputs()) {
            throw new EnigmaException(String.format("OP-Code %s requires %d elements on the stack but only %d were " +
                    "found", op.getStringRepr(), op.getInputs(), prog.getStackSize()));
        }

        // TODO: Implement computational limits similar to GAS


        // Execution
        switch (op) {
            /*
            BEGIN SECTION: STORE AND LOAD
             */
            case ENIGMA_ARRAY_INT_STORE:
                value = Convert.truncate(prog.stackPop().content, 32);
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.I, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_UINT_STORE:
                value = Convert.truncate(prog.stackPop().content, 32);
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.U, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_LONG_STORE:
                value = prog.stackPop().content;
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.L, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_ULONG_STORE:
                value = prog.stackPop().content;
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.UL, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_FLOAT_STORE:
                value = prog.stackPop().content;
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.F, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_DOUBLE_STORE:
                value = prog.stackPop().content;
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.store(EnigmaProgram.MEM_TARGET_STORE.D, integerKey, value);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_INT_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_I, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_UINT_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_U, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_LONG_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_L, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_ULONG_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_UL, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_FLOAT_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_F, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_DOUBLE_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_D, integerKey);
                prog.stepForward();
                break;
            case ENIGMA_ARRAY_M_LOAD:
                key = Convert.truncate(prog.stackPop().content, 32); // truncated to 32bit, we allow 2^32 keys
                integerKey = Convert.bytesToInt(key);
                prog.load(EnigmaProgram.MEM_TARGET_GET.GET_M, integerKey);
                prog.stepForward();
                break;

            /*
            BEGIN SECTION: PUSHDATA
             */
            case ENIGMA_PUSHDATA:
                // This is tricky, a push opcode always is followed by one byte describing the length between 1 and
                // 64 and the data directy afterwards
                prog.stepForward();
                int numberToSweep = (int) prog.getCurrentOperation();
                if (numberToSweep > 64)
                    throw new EnigmaException(String.format("You can only push 64bit at once to the " +
                            "stack"));
                if (numberToSweep < 1)
                    throw new EnigmaException(String.format("You have to push at least 1 byte to the " +
                            "stack"));
                prog.stepForward();
                byte[] toPush = prog.sweepNextOperations(numberToSweep);
                prog.stackPush(new EnigmaProgram.StackElement(Convert.nullToEmptyPacked(toPush, 64/8), null)); // type==null means unknown
                break;
        }
    }

    public static byte[] execute(EnigmaProgram prog, boolean debug) {
        try {
            while (!prog.isStopped()) {
                stepProgram(prog);
            }

            if(debug){
                prog.dumpStack();
                prog.dumpStorage(EnigmaProgram.MEM_TARGET_STORE.U);
                prog.dumpStorage(EnigmaProgram.MEM_TARGET_STORE.I);
                prog.dumpStorage(EnigmaProgram.MEM_TARGET_STORE.UL);
                prog.dumpStorage(EnigmaProgram.MEM_TARGET_STORE.L);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}