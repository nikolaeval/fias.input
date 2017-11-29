package tech.nikolaev.fias.model;

public class AddressEntityAction {

    public enum Action {
        INDEX,
        CREATE,
        UPDATE,
        DELETE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private Action action;
    private AddressEntity entity;

    public AddressEntityAction(Action action, AddressEntity entity) {
        this.action = action;
        this.entity = entity;
    }

    public Action getAction() {
        return action;
    }

    public AddressEntity getEntity() {
        return entity;
    }

}
