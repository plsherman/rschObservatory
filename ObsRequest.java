import java.util.*;

public enum ObsRequest {
  NO_OP(0),
  OPEN_ROOF(1),
  CLOSE_ROOF(2),
  STOP_ROOF(3),
  TOGGLE_OVERRIDE_SCOPES_PARKED(4),
  PUSH_INVERTER_POWER_BUTTON(5, true),
  TOGGLE_POWER_SCOPE1_POWER1(6),
  TOGGLE_POWER_SCOPE1_POWER2(7),
  TOGGLE_SCOPES_PARKED_POWER(8),
  TOGGLE_POWER_SCOPE2(9),
  TOGGLE_POWER_SCOPE3(10),
  TOGGLE_POWER_COMPUTER1(11), // aka "ethernet"; ac="eth"
  WAKE_UP_ABE_LAPTOP(12, true),
  TOGGLE_POWER_NAS(13), // aka "Backup drive"
  WAKE_UP_PHIL(14, true),
  TOGGLE_LIGHTS(15), // ac="Lights"
  WAKE_UP_ABE_DESKTOP(16, true),
  REFRESH_DISPLAY(98),
  STOP_PROCESSING(99, true);

  public final int code;
  public final boolean handledImmediately;

  public static ObsRequest forCode(int code) {
    validate();
    ObsRequest event = CODE_TO_EVENT.get(code);
    if (event == null) {
      throw new IllegalArgumentException("No event with code=" + code);
    }
    return event;
  }

  String marshall() {
    return "" + code;
  }

  public static ObsRequest unmarshall(String codeStr) {
    if (codeStr == null) return ObsRequest.NO_OP;
    try {
      int code = Integer.parseInt(codeStr);
      ObsRequest event = CODE_TO_EVENT.get(code);
      if (event == null) {
        System.out.println("Ignoring request with code=" + code);
        event = ObsRequest.NO_OP;
      }
      return event;
    } catch (NumberFormatException e) {
      System.out.println("Ignoring request with invalid code='" + codeStr + "'");
      return ObsRequest.NO_OP;
    }
  }

  public static void validate() {
    if (CODE_TO_EVENT.isEmpty()) {
      for (ObsRequest event : ObsRequest.values()) {
        if (CODE_TO_EVENT.put(event.code, event) != null) {
          DUPLICATE_CODES.add(event.code);
        }
      }
    }
    if (!DUPLICATE_CODES.isEmpty()) {
      throw new IllegalStateException("Duplicate codes: " + DUPLICATE_CODES);
    }
  }

  private ObsRequest(int code) {
    this(code, false);
  }

  private ObsRequest(int code, boolean handledImmediately) {
    this.code = code;
    this.handledImmediately = handledImmediately;
  }

  private static final Map<Integer, ObsRequest> CODE_TO_EVENT = new HashMap<>();
  private static final Set<Integer> DUPLICATE_CODES = new HashSet<>();
}
