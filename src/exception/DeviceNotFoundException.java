package exception;

/** Thrown when a device with the given IMEI is not found in the registry. */
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String imei) {
        super("Device not found: No device registered with IMEI [" + imei + "]");
    }
}
