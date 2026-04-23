package com.example.mod_1_21_4;

/**
 * Зберігає конфігурацію мода (координати для chorus farm, номер анархії тощо)
 */
public class ModConfig {
    // Chorus Auto Farm координати
    public static class Position {
        public int x, y, z;

        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Position() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        @Override
        public String toString() {
            return x + ", " + y + ", " + z;
        }
    }

    public static Position chorusPos1 = new Position();
    public static Position chorusPos2 = new Position();
    public static int anarchyNumber = 1;
}
