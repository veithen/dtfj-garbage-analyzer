import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.ibm.dtfj.java.JavaClass;
import com.ibm.dtfj.java.JavaObject;
import com.ibm.dtfj.java.JavaReference;

public class MarkSet {
    private final Set<JavaObject> markedObjects = new HashSet<JavaObject>();
    private final Set<JavaClass> markedClasses = new HashSet<JavaClass>();
    
    /**
     * Queue of {@link JavaObject} and {@link JavaClass} instances that have been marked but the
     * references of which have not been processed yet. We need to use a queue here. Recursion would
     * not work because the recursion depth may be too large and cause stack overflows.
     */
    private final Queue<Object> queue = new LinkedList<Object>();
    
    private int count;
    
    public void mark(JavaObject object) throws Exception {
        markOne(object);
        markEnqueued();
    }
    
    private void markOne(JavaObject object) throws Exception {
        if (markedObjects.add(object)) {
            queue.add(object);
            count++;
            if (count % 1000 == 0) {
                System.out.println("Marked " + count + " objects (queue=" + queue.size() + ")");
            }
        }
    }
    
    public void mark(JavaClass clazz) throws Exception {
        markOne(clazz);
        markEnqueued();
    }
    
    private void markOne(JavaClass clazz) throws Exception {
        if (markedClasses.add(clazz)) {
            queue.add(clazz);
        }
    }
    
    public void mark(JavaReference ref) throws Exception {
        markOne(ref);
        markEnqueued();
    }

    private void markOne(JavaReference ref) throws Exception {
        Object target = ref.getTarget();
        if (target instanceof JavaClass) {
            markOne((JavaClass)target);
        } else {
            markOne((JavaObject)target);
        }
    }
    
    private void markEnqueued() throws Exception {
        Object object;
        while ((object = queue.poll()) != null) {
            Iterator references;
            if (object instanceof JavaClass) {
                references = ((JavaClass)object).getReferences();
            } else {
                references = ((JavaObject)object).getReferences();
            }
            for (Iterator it = references; it.hasNext(); ) {
                markOne((JavaReference)it.next());
            }
        }
    }
    
    public boolean isMarked(JavaObject object) {
        return markedObjects.contains(object);
    }
    
    public int getCount() {
        return count;
    }
}
