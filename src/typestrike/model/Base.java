package typestrike.model;

/** Encapsulation: the only way to change health is through a validated operation. */
public final class Base {
    public static final int MAX_HEALTH = 100;
    private int health = MAX_HEALTH;

    public void takeDamage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Damage cannot be negative");
        health = Math.max(0, health - amount);
    }

    public int getHealth() { return health; }
    public boolean isDestroyed() { return health == 0; }
}
