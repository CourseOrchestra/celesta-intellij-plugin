import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Generated;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.ICelesta;
import ru.curs.celesta.dbutils.BasicCursor;
import ru.curs.celesta.dbutils.CelestaGenerated;
import ru.curs.celesta.dbutils.CursorIterator;
import ru.curs.celesta.dbutils.ParameterizedViewCursor;
import ru.curs.celesta.score.ColumnMeta;
import ru.curs.celesta.score.ParameterizedView;

@Generated(
        value = "ru.curs.celesta.plugin.maven.CursorGenerator",
        date = "2020-12-15T22:20:18.869434"
)
@CelestaGenerated
public final class Fun<caret>cCursor extends ParameterizedViewCursor implements Iterable<FuncCursor> {
    private static final String GRAIN_NAME = "demo";

    private static final String OBJECT_NAME = "func";

    public final FuncCursor.Columns COLUMNS;

    private String order_id;

    {
        this.COLUMNS = new FuncCursor.Columns(callContext().getCelesta());
    }

    public FuncCursor(CallContext context, Map<String, Object> parameters) {
        super(context, parameters);
    }

    public FuncCursor(CallContext context, Map<String, Object> parameters,
            ColumnMeta<?>... columns) {
        super(context, parameters, columns);
    }

    @Deprecated
    public FuncCursor(CallContext context, Set<String> fields, Map<String, Object> parameters) {
        super(context, fields, parameters);
    }

    public String getOrder_id() {
        return this.order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
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
    protected void _parseResult(ResultSet rs) throws SQLException {
        if (this.inRec("order_id")) {
            this.order_id = rs.getString("order_id");
            if (rs.wasNull()) {
                this.order_id = null;
            }
        }
    }

    @Override
    public void _clearBuffer(boolean withKeys) {
        this.order_id = null;
    }

    @Override
    public Object[] _currentValues() {
        Object[] result = new Object[1];
        result[0] = this.order_id;
        return result;
    }

    @Override
    public FuncCursor _getBufferCopy(CallContext context, List<String> fields) {
        final FuncCursor result;
        if (Objects.isNull(fields)) {
            result = new FuncCursor(context, this.parameters);
        }
        else {
            result = new FuncCursor(context, new LinkedHashSet<>(fields), this.parameters);
        }
        result.copyFieldsFrom(this);
        return result;
    }

    @Override
    public void copyFieldsFrom(BasicCursor c) {
        FuncCursor from = (FuncCursor)c;
        this.order_id = from.order_id;
    }

    @Override
    public Iterator<FuncCursor> iterator() {
        return new CursorIterator<FuncCursor>(this);
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
    @Generated(
            value = "ru.curs.celesta.plugin.maven.CursorGenerator",
            date = "2020-12-15T22:20:18.870306"
    )
    @CelestaGenerated
    public static final class Columns {
        private final ParameterizedView element;

        public Columns(ICelesta celesta) {
            this.element = celesta.getScore().getGrains().get(GRAIN_NAME).getElements(ParameterizedView.class).get(OBJECT_NAME);
        }

        public ColumnMeta<String> order_id() {
            return (ColumnMeta<String>) this.element.getColumns().get("order_id");
        }
    }
}
