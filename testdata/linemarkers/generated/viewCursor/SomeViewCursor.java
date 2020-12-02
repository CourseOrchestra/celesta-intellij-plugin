import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.ICelesta;
import ru.curs.celesta.dbutils.BasicCursor;
import ru.curs.celesta.dbutils.CursorIterator;
import ru.curs.celesta.dbutils.MaterializedViewCursor;
import ru.curs.celesta.score.ColumnMeta;
import ru.curs.celesta.score.MaterializedView;

public final class Som<caret>eViewCursor extends MaterializedViewCursor implements Iterable<SomeViewCursor> {
    private static final String GRAIN_NAME = "demo";

    private static final String OBJECT_NAME = "SomeView";

    public final SomeViewCursor.Columns COLUMNS;

    private Integer surrogate_count;

    private String item_id;

    private Integer qty;

    {
        this.COLUMNS = new SomeViewCursor.Columns(callContext().getCelesta());
    }

    public SomeViewCursor(CallContext context) {
        super(context);
    }

    public SomeViewCursor(CallContext context, ColumnMeta<?>... columns) {
        super(context, columns);
    }

    @Deprecated
    public SomeViewCursor(CallContext context, Set<String> fields) {
        super(context, fields);
    }

    public Integer getSurrogate_count() {
        return this.surrogate_count;
    }

    public void setSurrogate_count(Integer surrogate_count) {
        this.surrogate_count = surrogate_count;
    }

    public String getItem_id() {
        return this.item_id;
    }

    public void setItem_id(String item_id) {
        this.item_id = item_id;
    }

    public Integer getQty() {
        return this.qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    @Override
    protected Object _getFieldValue(String name) {
        try {
            Field f = getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(this);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void _setFieldValue(String name, Object value) {
        try {
            Field f = getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(this, value);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object[] _currentKeyValues() {
        Object[] result = new Object[1];
        result[0] = this.item_id;
        return result;
    }

    @Override
    protected void _parseResult(ResultSet rs) throws SQLException {
        if (this.inRec("surrogate_count")) {
            this.surrogate_count = rs.getInt("surrogate_count");
            if (rs.wasNull()) {
                this.surrogate_count = null;
            }
        }
        if (this.inRec("item_id")) {
            this.item_id = rs.getString("item_id");
            if (rs.wasNull()) {
                this.item_id = null;
            }
        }
        if (this.inRec("qty")) {
            this.qty = rs.getInt("qty");
            if (rs.wasNull()) {
                this.qty = null;
            }
        }
    }

    @Override
    public void _clearBuffer(boolean withKeys) {
        if (withKeys) {
            this.item_id = null;
        }
        this.surrogate_count = null;
        this.qty = null;
    }

    @Override
    public Object[] _currentValues() {
        Object[] result = new Object[3];
        result[0] = this.surrogate_count;
        result[1] = this.item_id;
        result[2] = this.qty;
        return result;
    }

    @Override
    public SomeViewCursor _getBufferCopy(CallContext context, List<String> fields) {
        final SomeViewCursor result;
        if (Objects.isNull(fields)) {
            result = new SomeViewCursor(context);
        }
        else {
            result = new SomeViewCursor(context, new LinkedHashSet<>(fields));
        }
        result.copyFieldsFrom(this);
        return result;
    }

    @Override
    public void copyFieldsFrom(BasicCursor c) {
        SomeViewCursor from = (SomeViewCursor)c;
        this.surrogate_count = from.surrogate_count;
        this.item_id = from.item_id;
        this.qty = from.qty;
    }

    @Override
    public Iterator<SomeViewCursor> iterator() {
        return new CursorIterator<SomeViewCursor>(this);
    }

    @Override
    protected String _grainName() {
        return GRAIN_NAME;
    }

    @Override
    protected String _objectName() {
        return OBJECT_NAME;
    }

    @SuppressWarnings("unchecked")
    public static final class Columns {
        private final MaterializedView element;

        public Columns(ICelesta celesta) {
            this.element = celesta.getScore().getGrains().get(GRAIN_NAME).getElements(MaterializedView.class).get(OBJECT_NAME);
        }

        public ColumnMeta<Integer> surrogate_count() {
            return (ColumnMeta<Integer>) this.element.getColumns().get("surrogate_count");
        }

        public ColumnMeta<String> item_id() {
            return (ColumnMeta<String>) this.element.getColumns().get("item_id");
        }

        public ColumnMeta<Integer> qty() {
            return (ColumnMeta<Integer>) this.element.getColumns().get("qty");
        }
    }
}
