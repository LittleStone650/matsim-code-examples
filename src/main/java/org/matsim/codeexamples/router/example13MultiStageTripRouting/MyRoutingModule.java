/* *********************************************************************** *
 * project: org.matsim.*
 * MyRoutingModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.codeexamples.router.example13MultiStageTripRouting;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.Facility;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link RoutingModule} for a mode consisting in going to a teleportation
 * station by public transport, and being instantly teleported to destination
 * 
 * @author thibautd
 */
public class MyRoutingModule implements RoutingModule {
	public static final String STAGE = "pr interaction";
	public static final String TELEPORTATION_LEG_MODE = "prleg";

	public static final String TELEPORTATION_MAIN_MODE = "pAr";

	private final Provider<RoutingModule> routingDelegate;
	private final PopulationFactory populationFactory;
	private final RouteFactories modeRouteFactory;
	private final Facility station;

	/**
	 * Creates a new instance.
	 * @param routingDelegate the {@link TripRouter} to use to compute the PT subtrips
	 * @param populationFactory used to create legs, activities and routes
	 * @param station {@link Facility} representing the teleport station
	 */
	public MyRoutingModule(
			// I do not know what is best here: RoutingModule or TripRouter.
			// RoutingModule is the level we actually need, but
			// getting the TripRouter allows to be consistent with modifications
			// of the TripRouter done later in the initialization process (delegation).
			// Using TripRouter may also lead to infinite loops, if two  modes
			// calling each other (though I cannot think in any actual mode with this risk).
			final Provider<RoutingModule> routingDelegate,
			final PopulationFactory populationFactory,
			final Facility station) {
		this.routingDelegate = routingDelegate;
		this.populationFactory = populationFactory;
		this.modeRouteFactory = populationFactory.getRouteFactories();
		this.station = station;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {

		final List<PlanElement> trip = new ArrayList<>(routingDelegate.get().calcRoute(request));
		final double departureTime = request.getDepartureTime();


		//如果trip中第一个leg的mode是walk，并且它的endlinkId 是station的linkId，那么
		//修改这个leg的endlinkId为station的linkId

		for (PlanElement planElement : trip) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (TransportMode.walk.equals(leg.getMode()) && station.getLinkId().equals(leg.getRoute().getEndLinkId())) {
					leg.getRoute().setEndLinkId(station.getLinkId());
				}
			}
		}

		PlanElement tempTrip = trip.get(3);
		if (tempTrip instanceof Leg) {
			Leg leg = (Leg) tempTrip;
			if (TransportMode.train.equals(leg.getMode())) {
				leg.getRoute().setEndLinkId(station.getLinkId());
			}
		}


		// create a dummy activity at the teleportation origin
		final Activity interaction = populationFactory.createActivityFromLinkId(
						STAGE, station.getLinkId());

		interaction.setMaximumDuration( 0 );
		// 这里添加activity
		trip.add(0, interaction );

		// create the teleportation leg
		final Leg teleportationLeg = populationFactory.createLeg( TELEPORTATION_LEG_MODE );
		teleportationLeg.setTravelTime( 0 );
		teleportationLeg.setDepartureTime( departureTime );
		final Route teleportationRoute = modeRouteFactory.createRoute(
						Route.class, request.getFromFacility().getLinkId(),station.getLinkId());

		teleportationRoute.setTravelTime( 0 );
		teleportationLeg.setRoute( teleportationRoute );
//		trip.remove(0);
		// 这里添加leg
		trip.add(0, teleportationLeg );


//		//creat walk interaction
//		final Activity walkInteraction = populationFactory.createActivityFromLinkId(
//				STAGE, station.getLinkId());
//		walkInteraction.setMaximumDuration( 0 );
//		trip.add(1, walkInteraction );
//
//		// create the walk leg
//		final Leg walkLeg = populationFactory.createLeg( TransportMode.walk );
//		teleportationLeg.setTravelTime( 0 );
//		//看trip中的第四个元素是不是train的leg，如果是是的话，获取它的startLinkId
//
//
//
//
//
//		Id<Link> Startlink = null;
//		if (trip.size() > 5) {
//			System.out.println("trip size is less than 4");
//
//			PlanElement tempTrip = trip.get(3);
//			if (tempTrip instanceof Leg) {
//				Leg leg = (Leg) tempTrip;
//				if (TransportMode.train.equals(leg.getMode())) {
//					Startlink = leg.getRoute().getStartLinkId();
//				}
//			}
//		}else {
//			System.out.println("trip size is less than 4");
//		}




//		final Route walkRoute = modeRouteFactory.createRoute(
//				Route.class, station.getLinkId(),Startlink);
//		walkRoute.setTravelTime( 0 );
//		walkLeg.setRoute( walkRoute );
//		trip.add(2, walkLeg );

		return trip;
	}
}

