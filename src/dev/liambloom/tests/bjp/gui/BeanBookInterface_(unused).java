//package dev.liambloom.tests.bjp.gui;
//
//import dev.liambloom.tests.bjp.shared.Result;
//import dev.liambloom.tests.bjp.shared.TestValidationStatus;
//import javafx.beans.InvalidationListener;
//import javafx.beans.property.*;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//
//import java.nio.file.Path;
//import java.util.Optional;
//
//public interface BeanBookInterface {
//    boolean isModifiable();
//
//    boolean hasPath();
//
//    String getName();
//
//    void setName(String name);
//
//    StringProperty nameProperty();
//
//    Optional<Path> getPath();
//
//    void setPath(Path path);
//
//    ObjectProperty<Optional<Path>> pathProperty();
//
//    Result getValidationResult();
//
//    ReadOnlyObjectProperty<Result> validationResultProperty();
//
//    boolean getExists();
//
//    ReadOnlyBooleanProperty existsProperty();
//
//    static BeanBook dummy() {
//        return Dummy.SINGLETON;
//    }
//}
//
//class Dummy implements BeanBookInteface {
//    public static final BeanBook SINGLETON = new Dummy();
//    private static final StringProperty NAME = new StringProperty() {
//        @Override
//        public String get() {
//            return "";
//        }
//
//        @Override
//        public void set(String value) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public void bind(ObservableValue<? extends String> observableValue) {
//            throw new UnsupportedOperationException("Cannot bind dummy property");
//        }
//
//        @Override
//        public boolean isBound() {
//            return false;
//        }
//
//        @Override
//        public Object getBean() {
//            return null;
//        }
//
//        @Override
//        public String getName() {
//            return "";
//        }
//
//        @Override
//        public void addListener(InvalidationListener invalidationListener) {}
//
//        @Override
//        public void removeListener(InvalidationListener invalidationListener) {}
//
//        @Override
//        public void addListener(ChangeListener<? super String> changeListener) {}
//
//        @Override
//        public void removeListener(ChangeListener<? super String> changeListener) {}
//
//        @Override
//        public void unbind() {}
//    };
//    private static final ObjectProperty<Optional<Path>> PATH = new ObjectProperty<>() {
//        @Override
//        public Optional<Path> get() {
//            return Optional.empty();
//        }
//
//        @Override
//        public void set(Optional<Path> value) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public void bind(ObservableValue<? extends Optional<Path>> observableValue) {
//            throw new UnsupportedOperationException("Cannot bind dummy property");
//        }
//
//        @Override
//        public boolean isBound() {
//            return false;
//        }
//
//        @Override
//        public Object getBean() {
//            return null;
//        }
//
//        @Override
//        public String getName() {
//            return "";
//        }
//
//        @Override
//        public void addListener(InvalidationListener invalidationListener) {}
//
//        @Override
//        public void removeListener(InvalidationListener invalidationListener) {}
//
//        @Override
//        public void addListener(ChangeListener<? super Optional<Path>> changeListener) {}
//
//        @Override
//        public void removeListener(ChangeListener<? super Optional<Path>> changeListener) {}
//
//        @Override
//        public void unbind() {}
//    };
//    private static final ReadOnlyObjectProperty<Result> VALIDATION_RESULT = new ReadOnlyObjectWrapper<>(new Result("", TestValidationStatus.NOT_FOUND)).getReadOnlyProperty();
//    private static final ReadOnlyBooleanProperty EXISTS = new ReadOnlyBooleanWrapper(false).getReadOnlyProperty();
//
//    private Dummy() {}
//
//    @Override
//    public boolean isModifiable() {
//        return false;
//    }
//
//    @Override
//    public boolean hasPath() {
//        return false;
//    }
//
//    @Override
//    public StringProperty nameProperty() {
//        return NAME;
//    }
//
//    @Override
//    public ObjectProperty<Optional<Path>> pathProperty() {
//        return PATH;
//    }
//
//    @Override
//    public ReadOnlyObjectProperty<Result> validationResultProperty() {
//        return VALIDATION_RESULT;
//    }
//
//    @Override
//    public ReadOnlyBooleanProperty existsProperty() {
//        return EXISTS;
//    }
//
//    @Override
//    public String getName() {
//        return nameProperty().get();
//    }
//}
