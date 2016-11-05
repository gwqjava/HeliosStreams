package com.heliosapm.metrichub.speedment.tsdb.public_.tsd_fqn_tagpair.generated;

import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_fqn_tagpair.TsdFqnTagpair;
import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagpair.TsdTagpair;
import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tsmeta.TsdTsmeta;
import com.speedment.runtime.core.manager.Manager;
import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Generated;

/**
 * The generated base implementation of the {@link
 * com.heliosapm.metrichub.speedment.tsdb.public_.tsd_fqn_tagpair.TsdFqnTagpair}-interface.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@Generated("Speedment")
public abstract class GeneratedTsdFqnTagpairImpl implements TsdFqnTagpair {
    
    private Long fqnTpId;
    private Long fqnid;
    private String xuid;
    private short porder;
    private String node;
    
    protected GeneratedTsdFqnTagpairImpl() {
        
    }
    
    @Override
    public Long getFqnTpId() {
        return fqnTpId;
    }
    
    @Override
    public Long getFqnid() {
        return fqnid;
    }
    
    @Override
    public String getXuid() {
        return xuid;
    }
    
    @Override
    public short getPorder() {
        return porder;
    }
    
    @Override
    public String getNode() {
        return node;
    }
    
    @Override
    public TsdFqnTagpair setFqnTpId(Long fqnTpId) {
        this.fqnTpId = fqnTpId;
        return this;
    }
    
    @Override
    public TsdFqnTagpair setFqnid(Long fqnid) {
        this.fqnid = fqnid;
        return this;
    }
    
    @Override
    public TsdFqnTagpair setXuid(String xuid) {
        this.xuid = xuid;
        return this;
    }
    
    @Override
    public TsdFqnTagpair setPorder(short porder) {
        this.porder = porder;
        return this;
    }
    
    @Override
    public TsdFqnTagpair setNode(String node) {
        this.node = node;
        return this;
    }
    
    @Override
    public TsdTsmeta findFqnid(Manager<TsdTsmeta> foreignManager) {
        return foreignManager.stream().filter(TsdTsmeta.FQNID.equal(getFqnid())).findAny().orElse(null);
    }
    
    @Override
    public TsdTagpair findXuid(Manager<TsdTagpair> foreignManager) {
        return foreignManager.stream().filter(TsdTagpair.XUID.equal(getXuid())).findAny().orElse(null);
    }
    
    @Override
    public String toString() {
        final StringJoiner sj = new StringJoiner(", ", "{ ", " }");
        sj.add("fqnTpId = " + Objects.toString(getFqnTpId()));
        sj.add("fqnid = " + Objects.toString(getFqnid()));
        sj.add("xuid = " + Objects.toString(getXuid()));
        sj.add("porder = " + Objects.toString(getPorder()));
        sj.add("node = " + Objects.toString(getNode()));
        return "TsdFqnTagpairImpl " + sj.toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (!(that instanceof TsdFqnTagpair)) { return false; }
        final TsdFqnTagpair thatTsdFqnTagpair = (TsdFqnTagpair)that;
        if (!Objects.equals(this.getFqnTpId(), thatTsdFqnTagpair.getFqnTpId())) {return false; }
        if (!Objects.equals(this.getFqnid(), thatTsdFqnTagpair.getFqnid())) {return false; }
        if (!Objects.equals(this.getXuid(), thatTsdFqnTagpair.getXuid())) {return false; }
        if (this.getPorder() != thatTsdFqnTagpair.getPorder()) {return false; }
        if (!Objects.equals(this.getNode(), thatTsdFqnTagpair.getNode())) {return false; }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(getFqnTpId());
        hash = 31 * hash + Objects.hashCode(getFqnid());
        hash = 31 * hash + Objects.hashCode(getXuid());
        hash = 31 * hash + Short.hashCode(getPorder());
        hash = 31 * hash + Objects.hashCode(getNode());
        return hash;
    }
}