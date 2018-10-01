package ca.mcgill.ecse321.rideshare9.repository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ca.mcgill.ecse321.rideshare9.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse321.rideshare9.entity.Advertisement;
import ca.mcgill.ecse321.rideshare9.entity.Stop;
import ca.mcgill.ecse321.rideshare9.entity.helper.AdvBestQuery;
import ca.mcgill.ecse321.rideshare9.entity.helper.AdvQuery;
import ca.mcgill.ecse321.rideshare9.entity.helper.AdvResponse;
import ca.mcgill.ecse321.rideshare9.service.impl.UserServiceImpl;
@Repository
public class AdvertisementRepository {
	@Autowired
	protected EntityManager em;
	@Autowired
	protected VehicleRepository vrp; 
	@Autowired
	protected StopRepository srp; 
	@Autowired
	protected UserServiceImpl urp; 

	
	@Transactional
	public Advertisement createAdv(String title, Date startTime, String startLocation, int seatAvailable, Set<Long> stops, long vehicle, long driver) {
		Advertisement adv = new Advertisement(); 
		adv.setTitle(title); 
		adv.setStartLocation(startLocation);
		adv.setSeatAvailable(seatAvailable);
		adv.setVehicle(vehicle);
		adv.setStartTime(startTime);
		adv.setDriver(driver);
		adv.setStops(stops);
		adv.setStatus(TripStatus.REGISTERING);
		em.persist(adv);
		em.flush();
	    return adv;
	}
	
	/**
	 * Driver change content of advertisement
	 * @param Advertisement to be changed
	 * @return advertisement changed
	 */
	@Transactional
	public Advertisement updateAdv(Advertisement adv) {
		em.merge(adv); 
		em.flush();
	    return adv;
	}
	
	/**
	 * Driver confirm ride start
	 * @param Advertisement id of adv to be ride
	 * @return advertisement started
	 */
	@Transactional
	public Advertisement rideAdv(long id) {
		Advertisement adv = this.findAdv(id); 
		adv.setStatus(TripStatus.ON_RIDE);
		em.merge(adv); 
		em.flush(); 
	    return adv;
	}
	
	/**
	 * Driver can cancel an adv
	 * @param Advertisement id of adv to be cancel
	 * @return advertisement canceled
	 */
	@Transactional
	public Advertisement cancelAdv(long id) {
		Advertisement adv = this.findAdv(id); 
		adv.setStatus(TripStatus.CANCELLED);
		em.merge(adv); 
		em.flush(); 
	    return adv;
	}
	
	/**
	 * If registration full, change status to closed
	 * FOR YUDI/Mapper: check seat available each time you add a passenger
	 * @param Advertisement id of adv to be closed
	 * @return advertisement closed
	 */
	@Transactional
	public Advertisement closeAdv(long id) {
		Advertisement adv = this.findAdv(id); 
		adv.setStatus(TripStatus.CLOSED);
		em.merge(adv); 
		em.flush(); 
	    return adv;
	}
	/**
	 * If journey finish, change status to complete
	 * @param Advertisement id of adv to be completed
	 * @return advertisement completed
	 */
	@Transactional
	public Advertisement completeAdv(long id) {
		Advertisement adv = this.findAdv(id); 
		adv.setStatus(TripStatus.COMPLETE);
		em.merge(adv); 
		em.flush(); 
	    return adv;
	}
	
	/**
	 * Find advertisement by id
	 * @param Advertisement id of adv to be found
	 * @return advertisement found
	 */
	@Transactional
	public Advertisement findAdv(long id) {
	    return em.find(Advertisement.class, id);
	}
	
	/**
	 * Remove advertisement by id
	 * @param Advertisement id of adv to be removed
	 * @return void
	 */
	@Transactional
	public Advertisement removeAdv(long id) {
		Advertisement adv = findAdv(id);
	    if (adv != null) {
	      em.remove(adv);
	    }
	    return adv; 
	}
	
	/**
	 * FOR YUDI/Mapper editor: sign up for a journey:: decrement seat available
	 * @param Advertisement id of adv to be registered
	 * @return advertisement registered with available seat decremented
	 */
	@Transactional 
	public Advertisement signUpAdv(long id) {
		Advertisement curr = this.findAdv(id); 
		curr.setSeatAvailable(curr.getSeatAvailable() - 1);
		em.merge(curr); 
		em.flush();
		return curr; 
	}
	
	/**
	 * List all advertisement posted
	 * @param void
	 * @return list of best driver and his count
	 */
	@Transactional
	public List<Advertisement> findAllAdv() {
	    TypedQuery<Advertisement> query = em.createQuery("SELECT a FROM Advertisement a", Advertisement.class);
	    return query.getResultList();
	}
	
	/**
	 * Sort Driver by count of advertisement posted
	 * @param void
	 * @return list of best driver and his count
	 */
	@Transactional
	public List<AdvBestQuery> findBestDriver() {
	    Query query = em.createQuery("SELECT a.driver, COUNT(a.id) FROM Advertisement a GROUP BY a.driver ORDER BY COUNT(a.id) DESC", Object[].class);
	    List<Object[]> did_list = query.getResultList(); 
	    ArrayList<AdvBestQuery> q = new ArrayList<AdvBestQuery>(); 
	    for (Object[] i: did_list) {
	    	AdvBestQuery currq = new AdvBestQuery(); 
            currq.setBest(urp.findUserByUID((Long)i[0]));
            currq.setCount((Long)i[1]);
            q.add(currq); 
	    }
	    return q;
	}
	
	/**
	 * Sort Advertisement by criteria price
	 * @param AdvQuery q
	 * @return result sorted by price, and within each price, sorted by time
	 */
	@Transactional
	public List<AdvResponse> findAdvByCriteriaSortByPrice(AdvQuery q) {		
	    TypedQuery<Advertisement> query = em.createQuery("SELECT a FROM Advertisement a WHERE a.startLocation LIKE :qLocation AND a.startTime BETWEEN :start AND :end ORDER BY a.startTime ASC", Advertisement.class)
	    		.setParameter("qLocation", "%" + q.getStartLocation() + "%").setParameter("start", q.getStartTimeX(), TemporalType.DATE).setParameter("end", q.getStartTimeY(), TemporalType.DATE);
	    TypedQuery<Stop> query2 = em.createQuery("SELECT s FROM Stop s WHERE s.stopName LIKE :qName ORDER BY s.price ASC", Stop.class).setParameter("qName", "%" + q.getStop() + "%");
	    ArrayList<Advertisement> resAdv = (ArrayList<Advertisement>)query.getResultList(); 
	    List<Stop> resStop = query2.getResultList(); 
	    ArrayList<AdvResponse> ret1 = new ArrayList<AdvResponse>(); 
	    for (Stop st: resStop) {
	    	for (Advertisement ad: resAdv) {
		    	if (ad.getStops().contains(st.getId())) {
		    		 ret1.add((new AdvResponse(ad, st.getPrice()))); 
		    	}
		    }
	    }
	    return ret1;
	}
	
	/**
	 * Sort Advertisement by criteria time
	 * @param AdvQuery q
	 * @return result sorted by time, and within each time, sorted by price
	 */
	@Transactional
	public List<AdvResponse> findAdvByCriteriaSortByTime(AdvQuery q) {		
	    TypedQuery<Advertisement> query = em.createQuery("SELECT a FROM Advertisement a WHERE a.startLocation LIKE :qLocation AND a.startTime BETWEEN :start AND :end ORDER BY a.startTime ASC", Advertisement.class)
	    		.setParameter("qLocation", "%" + q.getStartLocation() + "%").setParameter("start", q.getStartTimeX(), TemporalType.DATE).setParameter("end", q.getStartTimeY(), TemporalType.DATE);
	    TypedQuery<Stop> query2 = em.createQuery("SELECT s FROM Stop s WHERE s.stopName LIKE :qName ORDER BY s.price ASC", Stop.class).setParameter("qName", "%" + q.getStop() + "%");
	    ArrayList<Advertisement> resAdv = (ArrayList<Advertisement>)query.getResultList(); 
	    List<Stop> resStop = query2.getResultList(); 
	    ArrayList<AdvResponse> ret1 = new ArrayList<AdvResponse>(); 
	    for (Advertisement ad: resAdv) {
	    	for (Stop st: resStop) {
		    	if (ad.getStops().contains(st.getId())) {
		    		 ret1.add((new AdvResponse(ad, st.getPrice()))); 
		    	}
		    }
	    }
	    return ret1;
	}
}


