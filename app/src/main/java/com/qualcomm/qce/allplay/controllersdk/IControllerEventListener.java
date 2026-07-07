package com.qualcomm.qce.allplay.controllersdk;

public interface IControllerEventListener {
    default void onDeviceAdded(Device device) {}
    default void onDeviceRemoved(Device device) {}
    default void onZoneAdded(Zone zone) {}
    default void onZoneRemoved(Zone zone) {}
    default void onZonePlayerStateChanged(Zone zone, PlayerState state) {}
    default void onPlaylistChanged(Playlist playlist) {}
    default void onPlayerVolumeStateChanged(Player player, int volume) {}
    default void onPlaylistLoopStateChanged(Playlist playlist, LoopMode mode) {}
    default void onPlaylistShuffleStateChanged(Playlist playlist, ShuffleMode mode) {}
    default void onDeviceConnectionStateChanged(Device device, ConnectionState state) {}
    default void onPlayerDisplayNameChanged(Player player, String name) {}
    default void onOnboardingStateChanged(String deviceID, OnboardingState state) {}
    default void onZonePlaybackError(Zone zone, int index, Error code, String desc) {}
    default void onZonePlayersListChanged(Zone zone) {}
    default void onZoneIDChanged(Zone zone, String oldID) {}
    default void onZoneControlsEnabledChanged(Zone zone) {}
    default void onPlayerInterruptibleChanged(Player p, boolean i) {}
    default void onPlayerPartyModeEnabledChanged(Player p, boolean e) {}
    default void onPlayerVolumeEnabledChanged(Player p, boolean e) {}
    default void onPlayerInputSelectorChanged(Player p, String input) {}
    default void onDeviceDisplayNameChanged(Device d, String n) {}
    default void onDeviceBatteryStatusChanged(Device d, boolean bat, int cl, int tub, int tuc) {}
    default void onDeviceUpdateStarted(Device d) {}
    default void onDeviceAutoUpdateChanged(Device d, boolean a) {}
    default void onDeviceUpdateAvailable(Device d) {}
    default void onDeviceUpdateStatusChanged(Device d, UpdateStatus s) {}
    default void onDeviceUpdateProgressChanged(Device d, double p) {}
    default void onDeviceUpdatePhysicalRebootRequired(Device d) {}
    default UserPassword onDevicePasswordRequested(Device d) { return new UserPassword(); }
}
