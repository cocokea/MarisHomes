package dev.maris.homes.db;

import java.util.UUID;

public record HomeData(UUID uuid, int slot, String world, double x, double y, double z, float yaw, float pitch) {}
