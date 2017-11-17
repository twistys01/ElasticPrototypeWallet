/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017 The XEL Core Developers
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt;

import nxt.AccountLedger.LedgerEvent;
import nxt.computation.ComputationConstants;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.Logger;
import org.h2.schema.Constant;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static nxt.TransactionType.SUBTYPE_PAYMENT_REDEEM;
import static nxt.TransactionType.TYPE_PAYMENT;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountNQT;
    private final long totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private volatile List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Constants.INITIAL_BASE_TARGET;
    private long powTarget = 0;
    private int powLastMass = 0;
    private int powMass = 0;
    private long targetLastMass = 0;
    private long targetMass = 0;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private volatile byte[] bytes = null;


    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, String secretPhrase) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, null, previousBlockHash, transactions);
        blockSignature = Crypto.sign(bytes(), secretPhrase);
        bytes = null;
    }

    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        if(this.previousBlockId==0)
            this.height = 0;

        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;

        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
        }
    }

    @Override
    public byte[] getChecksum(int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();

        Iterator<TransactionImpl> iterator = this.blockTransactions.iterator();
        while (iterator.hasNext()) {
            digest.update(iterator.next().bytes());
        }


        byte[] checksum = digest.digest();
        return checksum;
    }


    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long powTarget, int
                      powLastMass, int powMass, long
                      targetLastMass, long targetMass, long
              nextBlockId, int height, long id,
              List<TransactionImpl> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                null, generationSignature, blockSignature, previousBlockHash, null);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.powTarget = powTarget;
        this.powLastMass = powLastMass;
        this.powMass = powMass;
        this.targetLastMass = targetLastMass;
        this.targetMass = targetMass;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        this.generatorId = generatorId;
        this.blockTransactions = blockTransactions;
    }

    @Override
    public void setLocallyProcessed() {

        BlockDb.updateBlockLocallyProcessed(this);
        BlockDb.updateBlockPowTargets( this );

    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public long getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        if (this.blockTransactions == null) {
            List<TransactionImpl> transactions = Collections.unmodifiableList(TransactionDb.findBlockTransactions(getId()));
            for (TransactionImpl transaction : transactions) {
                transaction.setBlock(this);
            }
            this.blockTransactions = transactions;
        }
        return this.blockTransactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public long getPowTarget() {
        return powTarget;
    }

    @Override
    public int getPowLastMass() {
        return powLastMass;
    }

    @Override
    public int getPowMass() {
        return powMass;
    }

    @Override
    public long getTargetLastMass() {
        return targetLastMass;
    }

    @Override
    public long getTargetMass() {
        return targetMass;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(bytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Long.toUnsignedString(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Long.toUnsignedString(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT);
        json.put("totalFeeNQT", totalFeeNQT);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        getTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        return json;
    }

    @Override
    public long getPreviousBlockPowTarget() {
        try {
            BlockImpl previousBlock = BlockchainImpl.getInstance().getBlock(getPreviousBlockId());
            return previousBlock.getPowTarget();
        }catch(Exception e){
            return 0;
        }
    }

    static BlockImpl parseBlock(JSONObject blockData) throws NxtException.NotValidException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            List<TransactionImpl> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, blockTransactions);
            if (!block.checkSignature()) {
                throw new NxtException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (NxtException.NotValidException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse block: " + blockData.toJSONString());
            throw e;
        }
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    byte[] bytes() {
        if (bytes == null) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + (blockSignature != null ? 64 : 0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(getTransactions().size());
            if (version < 3) {
                buffer.putInt((int) (totalAmountNQT / Constants.ONE_NXT));
                buffer.putInt((int) (totalFeeNQT / Constants.ONE_NXT));
            } else {
                buffer.putLong(totalAmountNQT);
                buffer.putLong(totalFeeNQT);
            }
            buffer.putInt(payloadLength);
            buffer.put(payloadHash);
            buffer.put(getGeneratorPublicKey());
            buffer.put(generationSignature);
            if (version > 1) {
                buffer.put(previousBlockHash);
            }
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            bytes = buffer.array();
        }
        return bytes;
    }

    boolean verifyBlockSignature() {
        return checkSignature() && Account.setOrVerify(getGeneratorId(), getGeneratorPublicKey());
    }

    boolean verifyBlockSignatureDebug() {
        return checkSignature();
    }

    private volatile boolean hasValidSignature = false;

    private boolean checkSignature() {
        if (! hasValidSignature) {
            byte[] data = Arrays.copyOf(bytes(), bytes.length - 64);
            hasValidSignature = blockSignature != null && Crypto.verify(blockSignature, data, getGeneratorPublicKey(), version >= 3);
        }
        return hasValidSignature;
    }

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {

        try {

            BlockImpl previousBlock = BlockchainImpl.getInstance().getBlock(getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", this);
            }

            if(previousBlock.getHeight()<Constants.ALLOW_FAKE_FORGING_ON_REDEEM_UNTIL_BLOCK)
                for (final Transaction t : this.blockTransactions) if (t.getType().getType() == TYPE_PAYMENT && t.getType().getSubtype() == SUBTYPE_PAYMENT_REDEEM) return true;


            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, getGeneratorPublicKey(), false)) {
                return false;
            }

            Account account = Account.getAccount(getGeneratorId());
            long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceNXT();
            if (effectiveBalance <= 0) {
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            byte[] generationSignatureHash;
            if (version == 1) {
                generationSignatureHash = digest.digest(generationSignature);
            } else {
                digest.update(previousBlock.generationSignature);
                generationSignatureHash = digest.digest(getGeneratorPublicKey());
                if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                    return false;
                }
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            return Generator.verifyHit(hit, BigInteger.valueOf(effectiveBalance), previousBlock, timestamp)
                    || (this.height < Constants.TRANSPARENT_FORGING_BLOCK_5 && Arrays.binarySearch(badBlocks, this.getId()) >= 0);

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    private static final long[] badBlocks = new long[] {
            5113090348579089956L, 8032405266942971936L, 7702042872885598917L, -407022268390237559L, -3320029330888410250L,
            -6568770202903512165L, 4288642518741472722L, 5315076199486616536L, -6175599071600228543L};
    static {
        Arrays.sort(badBlocks);
    }

    void apply() {
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(getGeneratorPublicKey());
        long totalBackFees = 0;
        if (this.height > Constants.SHUFFLING_BLOCK) {
            long[] backFees = new long[3];
            for (TransactionImpl transaction : getTransactions()) {
                long[] fees = transaction.getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                Account previousGeneratorAccount = Account.getAccount(BlockDb.findBlockAtHeight(this.height - i - 1).getGeneratorId());
                Logger.logDebugMessage("Back fees %f XEL to forger at height %d", ((double)backFees[i])/Constants.ONE_NXT, this.height - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.BLOCK_GENERATED, getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceNQT(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            Logger.logDebugMessage("Fee reduced by %f XEL at height %d", ((double)totalBackFees)/Constants.ONE_NXT, this.height);
        }
        generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.BLOCK_GENERATED, getId(), totalFeeNQT - totalBackFees);
        generatorAccount.addToForgedBalanceNQT(totalFeeNQT - totalBackFees);
    }

    void setPrevious(BlockImpl block) {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block);
            this.calculatePowTarget(block);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (TransactionImpl transaction : getTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    void loadTransactions() {
        for (TransactionImpl transaction : getTransactions()) {
            transaction.bytes();
            transaction.getAppendages();
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {
        long prevBaseTarget = previousBlock.baseTarget;
        if (previousBlock.getHeight() < 2) { // Set to 2 here
            baseTarget = BigInteger.valueOf(prevBaseTarget)
                    .multiply(BigInteger.valueOf(this.timestamp - previousBlock.timestamp))
                    .divide(BigInteger.valueOf(60)).longValue();
            if (baseTarget < 0 || baseTarget > Constants.MAX_BASE_TARGET) {
                baseTarget = Constants.MAX_BASE_TARGET;
            }
            if (baseTarget < prevBaseTarget / 2) {
                baseTarget = prevBaseTarget / 2;
            }
            if (baseTarget == 0) {
                baseTarget = 1;
            }
            long twofoldCurBaseTarget = prevBaseTarget * 2;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (baseTarget > twofoldCurBaseTarget) {
                baseTarget = twofoldCurBaseTarget;
            }
        } else if (previousBlock.getHeight() % 2 == 0) {
            BlockImpl block = BlockDb.findBlockAtHeight(previousBlock.getHeight() - 2);
            int blocktimeAverage = (this.timestamp - block.timestamp) / 3;
            if (blocktimeAverage > 60) {
                baseTarget = (prevBaseTarget * Math.min(blocktimeAverage, Constants.MAX_BLOCKTIME_LIMIT)) / 60;
            } else {
                baseTarget = prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                        * (60 - Math.max(blocktimeAverage, Constants.MIN_BLOCKTIME_LIMIT)) / 6000;
            }
            if (baseTarget < 0 || baseTarget > Constants.MAX_BASE_TARGET_2) {
                baseTarget = Constants.MAX_BASE_TARGET_2;
            }
            if (baseTarget < Constants.MIN_BASE_TARGET) {
                baseTarget = Constants.MIN_BASE_TARGET;
            }
        } else {
            baseTarget = prevBaseTarget;
        }
        cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
    }

    private void calculatePowTarget(BlockImpl previousBlock) {
        int powcnt = 0;

        if(this.getHeight() < ComputationConstants.START_ENCODING_BLOCK){
            // Do nothing
            return;
        }
        else if(this.getHeight() < ComputationConstants.START_ENCODING_BLOCK + ComputationConstants
                .POW_RETARGET_DEPTH){
            powcnt = this.countPow();
            // Initialization Window. Just count and set target to MAX, don't adjust anything
            powTarget = Long.MAX_VALUE / 100;
            if(this.getHeight()==ComputationConstants.START_ENCODING_BLOCK)
            {
                powMass = powcnt;
                targetMass = powTarget;
            }else{
                powMass = powcnt + previousBlock.powMass;
                targetMass = previousBlock.targetMass + powTarget;
            }
            powLastMass = powcnt;
            targetLastMass = powTarget;

        }else{
            powcnt = this.countPow();
            // Regular handling with "rolling window"
            BlockImpl b = BlockDb.findBlockAtHeight(this.getHeight()-ComputationConstants.POW_RETARGET_DEPTH);
            powMass = powcnt + previousBlock.powMass - b.powLastMass;
            powLastMass = powcnt;

            long darkTarget = previousBlock.targetMass;
            darkTarget /= ComputationConstants.POW_RETARGET_DEPTH;
            int nActualTimespan = this.getTimestamp()-b.getTimestamp();

            // TODO: check implication of setting this to no-retarget in case no POW submissions come in
            int nTargetTimespan = nActualTimespan;
            if(powMass!=0)
                nTargetTimespan = powMass * (60 / ComputationConstants.WE_WANT_X_POW_PER_MINUTE);

            if (nActualTimespan < nTargetTimespan / 3)
                nActualTimespan = nTargetTimespan / 3;
            if (nActualTimespan > nTargetTimespan * 3)
                nActualTimespan = nTargetTimespan * 3;

            long tmp = darkTarget;
            darkTarget = (darkTarget / nTargetTimespan)*nActualTimespan;

            // Overflow safety, TODO: check if these overflows can be handled this way on signed long
            if((nActualTimespan>nTargetTimespan && darkTarget<tmp) || (darkTarget > Long.MAX_VALUE / 100)){
                darkTarget = Long.MAX_VALUE / 100;
            }
            if((nActualTimespan<nTargetTimespan && darkTarget>tmp) || (darkTarget < 1)){
                darkTarget = 1;
            }
            powTarget = darkTarget;
            targetMass = powTarget + previousBlock.targetMass - b.targetLastMass;
            targetLastMass = powTarget;
        }

        Logger.logInfoMessage("Block " + this.getHeight() + " POW retarget; powLastMass=" + powLastMass + ", powMass=" +
                        powMass + ", targetLastMass=" + targetLastMass + ", targetMass=" + targetMass + " -> TARGET = " + powTarget);



    }

    private int countPow() {
        return 0; // todo
    }

}
