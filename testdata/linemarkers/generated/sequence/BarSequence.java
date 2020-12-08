import javax.annotation.Generated;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.dbutils.CelestaGenerated;
import ru.curs.celesta.dbutils.Sequence;

@Generated(
        value = "ru.curs.celesta.plugin.maven.CursorGenerator",
        date = "2020-12-06T17:14:47.54392"
)
@CelestaGenerated
public final class  B<caret>arSequence extends Sequence {
    private static final String GRAIN_NAME = "example";

    private static final String OBJECT_NAME = "bar";

    public FodSequence(CallContext context) {
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
