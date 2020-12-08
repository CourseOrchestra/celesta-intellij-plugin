import javax.annotation.Generated;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.dbutils.CelestaGenerated;
import ru.curs.celesta.dbutils.Sequence;

@Generated(
        value = "ru.curs.celesta.plugin.maven.CursorGenerator",
        date = "2020-12-06T16:48:21.8448"
)
@CelestaGenerated
public final class FooSeque<caret>nce extends Sequence {
    private static final String GRAIN_NAME = "example";

    private static final String OBJECT_NAME = "foo";

    public FooSequence(CallContext context) {
        super(context);
    }

    @Override
    protected String _grainName() {
        return GRAIN_NAME;
    }

    @Override
    protected String _objectName() {
        return OBJECT_NAME;
    }
}
