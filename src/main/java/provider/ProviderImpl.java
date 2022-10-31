package provider;

import java.util.UUID;

public class ProviderImpl implements Provider {

    private final String uuid = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return uuid;
    }

    @Override
    public String get() {
        return getId();
    }

    @Override
    public String getTraceableId() {
        return getId();
    }

    @Override
    public boolean check() {
        return true;
    }
}
