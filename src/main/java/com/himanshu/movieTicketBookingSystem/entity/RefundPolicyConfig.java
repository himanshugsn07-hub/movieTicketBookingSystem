package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "refund_policy")
public class RefundPolicyConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    private boolean defaultPolicy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "refund_policy_tier", joinColumns = @JoinColumn(name = "policy_id"))
    private List<RefundTier> tiers = new ArrayList<>();

    protected RefundPolicyConfig() {
    }

    public RefundPolicyConfig(String name, List<RefundTier> tiers, boolean defaultPolicy) {
        this.name = name;
        this.tiers = new ArrayList<>(tiers);
        this.defaultPolicy = defaultPolicy;
    }

    // Returns the policy id.
    public int getId() {
        return id;
    }

    // Returns the policy name.
    public String getName() {
        return name;
    }

    // Returns true if this is the default policy for shows without their own.
    public boolean isDefaultPolicy() {
        return defaultPolicy;
    }

    // Returns the refund tiers.
    public List<RefundTier> getTiers() {
        return tiers;
    }

    // Marks or unmarks this policy as the default.
    public void setDefaultPolicy(boolean defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    // Replaces the policy's name and tiers.
    public void update(String name, List<RefundTier> tiers) {
        this.name = name;
        this.tiers = new ArrayList<>(tiers);
    }

    // Returns the tiers as a compact string, longest notice first, such as "24:100,6:50,0:0".
    public String toSnapshot() {
        return tiers.stream()
                .sorted(Comparator.comparingInt(RefundTier::hoursBeforeShow).reversed())
                .map(t -> t.hoursBeforeShow() + ":" + t.refundPercent())
                .collect(Collectors.joining(","));
    }
}
