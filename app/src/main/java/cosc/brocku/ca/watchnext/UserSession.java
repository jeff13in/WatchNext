package cosc.brocku.ca.watchnext;

public class UserSession {
    private static UserSession instance;
    private int supabaseUserId = -1;
    private String firebaseUid = "";
    private String email = "";

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public int getSupabaseUserId() { return supabaseUserId; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getEmail() { return email; }

    public void set(int supabaseUserId, String firebaseUid, String email) {
        this.supabaseUserId = supabaseUserId;
        this.firebaseUid    = firebaseUid;
        this.email          = email;
    }

    public boolean isLoaded() { return supabaseUserId != -1; }

    public void clear() {
        supabaseUserId = -1;
        firebaseUid    = "";
        email          = "";
    }
}
