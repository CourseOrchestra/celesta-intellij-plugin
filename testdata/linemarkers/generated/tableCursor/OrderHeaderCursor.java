import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.ICelesta;
import ru.curs.celesta.dbutils.BasicCursor;
import ru.curs.celesta.dbutils.Cursor;
import ru.curs.celesta.dbutils.CursorIterator;
import ru.curs.celesta.event.TriggerType;
import ru.curs.celesta.score.ColumnMeta;
import ru.curs.celesta.score.Table;

public final class OrderHead<caret>erCursor extends Cursor implements Iterable<OrderHeaderCursor> {
    private static final String GRAIN_NAME = "demo";

    private static final String OBJECT_NAME = "OrderHeader";

    public final OrderHeaderCursor.Columns COLUMNS;

    private String id;

    private Date date;

    private String customer_id;

    private String customer_name;

    private String manager_id;

    {
        this.COLUMNS = new OrderHeaderCursor.Columns(callContext().getCelesta());
    }

    public OrderHeaderCursor(CallContext context) {
        super(context);
    }

    public OrderHeaderCursor(CallContext context, ColumnMeta<?>... columns) {
        super(context, columns);
    }

    @Deprecated
    public OrderHeaderCursor(CallContext context, Set<String> fields) {
        super(context, fields);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCustomer_id() {
        return this.customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getCustomer_name() {
        return this.customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getManager_id() {
        return this.manager_id;
    }

    public void setManager_id(String manager_id) {
        this.manager_id = manager_id;
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
        result[0] = this.id;
        return result;
    }

    @Override
    protected void _parseResultInternal(ResultSet rs) throws SQLException {
        if (this.inRec("id")) {
            this.id = rs.getString("id");
            if (rs.wasNull()) {
                this.id = null;
            }
        }
        if (this.inRec("date")) {
            this.date = rs.getTimestamp("date");
            if (rs.wasNull()) {
                this.date = null;
            }
        }
        if (this.inRec("customer_id")) {
            this.customer_id = rs.getString("customer_id");
            if (rs.wasNull()) {
                this.customer_id = null;
            }
        }
        if (this.inRec("customer_name")) {
            this.customer_name = rs.getString("customer_name");
            if (rs.wasNull()) {
                this.customer_name = null;
            }
        }
        if (this.inRec("manager_id")) {
            this.manager_id = rs.getString("manager_id");
            if (rs.wasNull()) {
                this.manager_id = null;
            }
        }
        this.setRecversion(rs.getInt("recversion"));
    }

    @Override
    public void _clearBuffer(boolean withKeys) {
        if (withKeys) {
            this.id = null;
        }
        this.date = null;
        this.customer_id = null;
        this.customer_name = null;
        this.manager_id = null;
    }

    @Override
    public Object[] _currentValues() {
        Object[] result = new Object[5];
        result[0] = this.id;
        result[1] = this.date;
        result[2] = this.customer_id;
        result[3] = this.customer_name;
        result[4] = this.manager_id;
        return result;
    }

    @Override
    protected void _setAutoIncrement(int val) {
    }

    public static void onPreDelete(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.PRE_DELETE, OrderHeaderCursor.class, cursorConsumer);
    }

    public static void onPostDelete(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.POST_DELETE, OrderHeaderCursor.class, cursorConsumer);
    }

    public static void onPreInsert(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.PRE_INSERT, OrderHeaderCursor.class, cursorConsumer);
    }

    public static void onPostInsert(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.POST_INSERT, OrderHeaderCursor.class, cursorConsumer);
    }

    public static void onPreUpdate(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.PRE_UPDATE, OrderHeaderCursor.class, cursorConsumer);
    }

    public static void onPostUpdate(ICelesta celesta, Consumer<OrderHeaderCursor> cursorConsumer) {
        celesta.getTriggerDispatcher().registerTrigger(TriggerType.POST_UPDATE, OrderHeaderCursor.class, cursorConsumer);
    }

    @Override
    public OrderHeaderCursor _getBufferCopy(CallContext context, List<String> fields) {
        final OrderHeaderCursor result;
        if (Objects.isNull(fields)) {
            result = new OrderHeaderCursor(context);
        }
        else {
            result = new OrderHeaderCursor(context, new LinkedHashSet<>(fields));
        }
        result.copyFieldsFrom(this);
        return result;
    }

    @Override
    public void copyFieldsFrom(BasicCursor c) {
        OrderHeaderCursor from = (OrderHeaderCursor)c;
        this.id = from.id;
        this.date = from.date;
        this.customer_id = from.customer_id;
        this.customer_name = from.customer_name;
        this.manager_id = from.manager_id;
        this.setRecversion(from.getRecversion());
    }

    @Override
    public Iterator<OrderHeaderCursor> iterator() {
        return new CursorIterator<OrderHeaderCursor>(this);
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
        private final Table element;

        public Columns(ICelesta celesta) {
            this.element = celesta.getScore().getGrains().get(GRAIN_NAME).getElements(Table.class).get(OBJECT_NAME);
        }

        public ColumnMeta<String> id() {
            return (ColumnMeta<String>) this.element.getColumns().get("id");
        }

        public ColumnMeta<Date> date() {
            return (ColumnMeta<Date>) this.element.getColumns().get("date");
        }

        public ColumnMeta<String> customer_id() {
            return (ColumnMeta<String>) this.element.getColumns().get("customer_id");
        }

        public ColumnMeta<String> customer_name() {
            return (ColumnMeta<String>) this.element.getColumns().get("customer_name");
        }

        public ColumnMeta<String> manager_id() {
            return (ColumnMeta<String>) this.element.getColumns().get("manager_id");
        }
    }
}
