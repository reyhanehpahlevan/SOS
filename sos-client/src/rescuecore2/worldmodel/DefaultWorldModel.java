package rescuecore2.worldmodel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
   Default implementation of a WorldModel.
   @param <T> The subclass of Entity that this world model holds.
*/
public class DefaultWorldModel<T extends Entity> extends AbstractWorldModel<T> {
    private Map<EntityID, T> entities;
    private Collection<Class<? extends T>> allowedClasses;

    /**
       Construct an empty world model.
       @param clazz The class of objects that are allowed in this world model. This approach is a workaround for the limitations of Java generics.
    */
    public DefaultWorldModel(Class<? extends T> clazz) {
        entities = new HashMap<EntityID, T>();
        allowedClasses = new HashSet<Class<? extends T>>();
        allowedClasses.add(clazz);
    }

    /**
       Construct an empty world model.
       @return A new DefaultWorldModel that accepts any type of Entity.
    */
    public static DefaultWorldModel<Entity> create() {
        return new DefaultWorldModel<Entity>(Entity.class);
    }

    @Override
    public final Collection<Class<? extends T>> getAllowedClasses() {
        return allowedClasses;
    }

    @Override
    public final Collection<T> getAllEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    @Override
    public final void addEntityImpl(T e) {
    	if(!entities.containsKey(e.getID())){
    		entities.put(e.getID(), e);
    		fireEntityAdded(e);
        }
    }

    @Override
    public final void removeEntity(EntityID id) {
        T removed = entities.remove(id);
        if (removed != null) {
            fireEntityRemoved(removed);
        }
    }

    @Override
    public final void removeAllEntities() {
        Set<T> all = new HashSet<T>(entities.values());
        entities.clear();
        for (T next : all) {
            fireEntityRemoved(next);
        }
    }

	public final T getEntity(Integer id) {
        return entities.get(new EntityID(id));
    }

    @Override
    public final Iterator<T> iterator() {
        return entities.values().iterator();
    }

    @Override
    public void merge(ChangeSet changeSet, int time) {
	merge(changeSet);
	
    }

	
	@Override
	public T getEntity(EntityID id) {
		// TODO Auto-generated method stub
		return entities.get(id);

	}

}