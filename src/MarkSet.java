import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.dtfj.java.JavaClass;
import com.ibm.dtfj.java.JavaObject;
import com.ibm.dtfj.java.JavaReference;

public class MarkSet {
    private final Set<JavaObject> markedObjects = new HashSet<JavaObject>();
    private final Set<JavaClass> markedClasses = new HashSet<JavaClass>();
    private int count;
    
    public void mark(JavaObject object) throws Exception {
        if (markedObjects.add(object)) {
            count++;
            for (Iterator it = object.getReferences(); it.hasNext(); ) {
                mark((JavaReference)it.next());
            }
        }
    }
    
    public void mark(JavaClass clazz) throws Exception {
        if (markedClasses.add(clazz)) {
            for (Iterator it = clazz.getReferences(); it.hasNext(); ) {
                mark((JavaReference)it.next());
            }
        }
    }
    
    public void mark(JavaReference ref) throws Exception {
        Object target = ref.getTarget();
        if (target instanceof JavaClass) {
            mark((JavaClass)target);
        } else {
            mark((JavaObject)target);
        }
    }

    public boolean isMarked(JavaObject object) {
        return markedObjects.contains(object);
    }
    
    public int getCount() {
        return count;
    }
}
