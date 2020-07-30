package io.ghap.user.model;

public class Domain extends AbstractModel{

    private Integer maxPwdAge;
    private Integer minPwdAge;
    private Integer minPwdLength;
    private Integer lockoutThreshold;
    private String name;

    protected Domain() {
        super("domain");
    }

    public Domain(String dn){
        super("group");
        setDn(dn);
    }

    public Integer getMaxPwdAge() {
        return maxPwdAge;
    }

    public void setMaxPwdAge(Integer maxPwdAge) {
        this.maxPwdAge = maxPwdAge;
    }

    public Integer getMinPwdLength() {
        return minPwdLength;
    }

    public void setMinPwdLength(Integer minPwdLength) {
        this.minPwdLength = minPwdLength;
    }

    public void setMinPwdAge(Integer minPwdAge) {
        this.minPwdAge = minPwdAge;
    }

    public Integer getMinPwdAge() {
        return minPwdAge;
    }

    public Integer getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(Integer lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
