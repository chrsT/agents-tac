// TAC Agent Weir by Christopher Taylor
//		 ct345
//	   University of Bath
//	Computer Science Department
//	Agents Coursework 1, 2014-15

/**
 * TAC AgentWare
 * http://www.sics.se/tac        tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : 23 April, 2002
 * Updated : $Date: 2005/06/07 19:06:16 $
 *	     $Revision: 1.1 $
 * ---------------------------------------------------------
 * DummyAgent is a simplest possible agent for TAC. It uses
 * the TACAgent agent ware to interact with the TAC server.
 *
 * Important methods in TACAgent:
 *
 * Retrieving information about the current Game
 * ---------------------------------------------
 * int getGameID()
 *  - returns the id of current game or -1 if no game is currently plaing
 *
 * getServerTime()
 *  - returns the current server time in milliseconds
 *
 * getGameTime()
 *  - returns the time from start of game in milliseconds
 *
 * getGameTimeLeft()
 *  - returns the time left in the game in milliseconds
 *
 * getGameLength()
 *  - returns the game length in milliseconds
 *
 * int getAuctionNo()
 *  - returns the number of auctions in TAC
 *
 * int getClientPreference(int client, int type)
 *  - returns the clients preference for the specified type
 *   (types are TACAgent.{ARRIVAL, DEPARTURE, HOTEL_VALUE, E1, E2, E3}
 *
 * int getAuctionFor(int category, int type, int day)
 *  - returns the auction-id for the requested resource
 *   (categories are TACAgent.{CAT_FLIGHT, CAT_HOTEL, CAT_ENTERTAINMENT
 *    and types are TACAgent.TYPE_INFLIGHT, TACAgent.TYPE_OUTFLIGHT, etc)
 *
 * int getAuctionCategory(int auction)
 *  - returns the category for this auction (CAT_FLIGHT, CAT_HOTEL,
 *    CAT_ENTERTAINMENT)
 *
 * int getAuctionDay(int auction)
 *  - returns the day for this auction.
 *
 * int getAuctionType(int auction)
 *  - returns the type for this auction (TYPE_INFLIGHT, TYPE_OUTFLIGHT, etc).
 *
 * int getOwn(int auction)
 *  - returns the number of items that the agent own for this
 *    auction
 *
 * Submitting Bids
 * ---------------------------------------------
 * void submitBid(Bid)
 *  - submits a bid to the tac server
 *
 * void replaceBid(OldBid, Bid)
 *  - replaces the old bid (the current active bid) in the tac server
 *
 *   Bids have the following important methods:
 *    - create a bid with new Bid(AuctionID)
 *
 *   void addBidPoint(int quantity, float price)
 *    - adds a bid point in the bid
 *
 * Help methods for remembering what to buy for each auction:
 * ----------------------------------------------------------
 * int getAllocation(int auctionID)
 *   - returns the allocation set for this auction
 * void setAllocation(int auctionID, int quantity)
 *   - set the allocation for this auction
 *
 *
 * Callbacks from the TACAgent (caused via interaction with server)
 *
 * bidUpdated(Bid bid)
 *  - there are TACAgent have received an answer on a bid query/submission
 *   (new information about the bid is available)
 * bidRejected(Bid bid)
 *  - the bid has been rejected (reason is bid.getRejectReason())
 * bidError(Bid bid, int error)
 *  - the bid contained errors (error represent error status - commandStatus)
 *
 * quoteUpdated(Quote quote)
 *  - new information about the quotes on the auction (quote.getAuction())
 *    has arrived
 * quoteUpdated(int category)
 *  - new information about the quotes on all auctions for the auction
 *    category has arrived (quotes for a specific type of auctions are
 *    often requested at once).

 * auctionClosed(int auction)
 *  - the auction with id "auction" has closed
 *
 * transaction(Transaction transaction)
 *  - there has been a transaction
 *
 * gameStarted()
 *  - a TAC game has started, and all information about the
 *    game is available (preferences etc).
 *
 * gameStopped()
 *  - the current game has ended
 *
 */

package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;

import java.util.logging.*;

import java.util.*;

public class DummyAgent extends AgentImpl {

  private static final Logger log =
    Logger.getLogger(DummyAgent.class.getName());

  private static final boolean DEBUG = false;

  private float[] prices;
  private int n_hotels_closed;
  private int[] items_available;
  private int[] hqw_available;
  private int[][] client_days;

  protected void init(ArgEnumerator args) {
    prices = new float[agent.getAuctionNo()];
    items_available = new int[agent.getAuctionNo()];
    client_days = new int[8][2];
    n_hotels_closed = 0;
  }

private void allocationBids() {
	log.fine("Allocate bids:n_hotels_closed == "+Integer.toString(n_hotels_closed)+", last_close == "+Integer.toString(last_close));
	if ((n_hotels_closed >= 0) && (n_hotels_closed > last_close))
	{
		last_close = n_hotels_closed;
		if (n_hotels_closed >= 1) {
       			log.fine("CALCULATING ALLOCATIONS AND SENDING BIDS"); 
			calculateAllocation(); 
			entertainmentBids();
		}
		flightBids(); 
		hotelBids();
	}
}

private float getBasicHotelAmount()
{
	return (300 + n_hotels_closed*25);
}

private void hotelBids() {
	log.fine("hotelBids();");
	for (int i = 8; i < 16; i++) {
		if (agent.getQuote(i).isAuctionClosed()) { continue; }
		Bid bid = new Bid(i);
		Quote quote = agent.getQuote(i);
		int hqw = quote.getHQW();
		float ask = quote.getBidPrice(); //TODO: getBidPrice() maybe?
		float bid_amount = 0;
		int alloc = agent.getAllocation(i);
		bid.addBidPoint(16,1); bid.addBidPoint(8,3); bid.addBidPoint(4,5); bid.addBidPoint(3,8); bid.addBidPoint(2,14); bid.addBidPoint(1,20);
		if ((hqw - alloc > 0) && (alloc > 0)) { bid.addBidPoint(hqw-alloc,ask+1); }
		if (alloc > 0) {
			bid_amount = getBasicHotelAmount();
			if (ask > bid_amount) { bid_amount = ask + (20*n_hotels_closed); }
			bid.addBidPoint(alloc,bid_amount);
		}
		agent.submitBid(bid);
	}
}

private void flightBids() {
	log.fine("flightBids();");
	for (int i = 0; i < 8; i++) {
		int alloc = agent.getAllocation(i);
		int owned = agent.getOwn(i);
		if (alloc > owned) {
			Bid bid = new Bid(i);
			bid.addBidPoint(alloc-owned,1000);
			agent.submitBid(bid);
		}
	}
}

private void entertainmentBids() {
	for (int auc = 16; auc < 28; auc++) {
		int owned = agent.getOwn(auc);
		int alloc = agent.getAllocation(auc);
		Bid bid = new Bid(auc);
		if (owned > alloc) { bid.addBidPoint(alloc-owned,sell_price); }
		if (alloc > owned) { bid.addBidPoint(alloc-owned,buy_price); } 
		agent.submitBid(bid);
	}
}

private int getEntertainmentUtil(int client, int day, int type)
{
	if ((client_days[client][0] <= day) && (client_days[client][1] > day)) {
		return agent.getClientPreference(client,type);
	}
	return 0; 
}

private int getLength(int client)
{
	return client_days[client][1] - client_days[client][0];
}

  public void quoteUpdated(Quote quote) {
    return;
  }

  public void quoteUpdated(int auctionCategory) {
    log.fine("All quotes for "
	     + agent.auctionCategoryToString(auctionCategory)
	     + " has been updated"); 
	  }

  public void bidUpdated(Bid bid) {
    log.fine("Bid Updated: id=" + bid.getID() + " auction="
	     + bid.getAuction() + " state="
	     + bid.getProcessingStateAsString());
    log.fine("       Hash: " + bid.getBidHash());
  }

  public void bidRejected(Bid bid) {
    log.warning("Bid Rejected: " + bid.getID());
    log.warning("      Reason: " + bid.getRejectReason()
		+ " (" + bid.getRejectReasonAsString() + ')');
  }

  public void bidError(Bid bid, int status) {
    log.warning("Bid Error in auction " + bid.getAuction() + ": " + status
		+ " (" + agent.commandStatusToString(status) + ')');
  }
  private boolean game_going;
  public void gameStarted() {
    log.fine("Game " + agent.getGameID() + " started!");
    game_going = true;
    n_hotels_closed = 0;
    last_close = -1;	
    items_available = new int[agent.getAuctionNo()];

    allocationBids();
    current_allocations = new int[8][3];
    }

  public void gameStopped() {
	game_going = false;
    log.fine("Game Stopped!");
  }

  private int last_close;
  public void auctionClosed(int auction) {
    log.fine("*** Auction " + auction + " closed!");
    int auction_type = agent.getAuctionCategory(auction);
    switch (auction_type)
    {
    case TACAgent.CAT_HOTEL:
	{
		n_hotels_closed += 1;
	}

    allocationBids();
    }
  }

  private void initialBids() {
	for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
		if (agent.getAuctionCategory(i) == TACAgent.CAT_HOTEL) {
			Bid bid = new Bid(i);
			bid.addBidPoint(16,1);
			agent.submitBid(bid);
		}
	}
  }
    
  
  private int predictUtility(int client, int day_in, int day_out, int hotel_type)
  {
	  int in_flight_pref = agent.getClientPreference(client,TACAgent.ARRIVAL);
	  int out_flight_pref = agent.getClientPreference(client,TACAgent.DEPARTURE);
	  int hotel_value = agent.getClientPreference(client,TACAgent.HOTEL_VALUE);
	  
	  int P_Utility = 1000;
	  
	  P_Utility -= (Math.abs(day_in-in_flight_pref)+Math.abs(day_out-out_flight_pref)) * 100; //TravelPenalty
	  
	  if (hotel_type == TACAgent.TYPE_GOOD_HOTEL)
	  {
		  P_Utility += hotel_value; //HotelBonus
	  }
	  
	  //Ignoring Entertainment in this calculation
	  return P_Utility-predictCost(day_in,day_out,hotel_type);
  }
  
  private int predictCost(int day_in, int day_out, int hotel_type)
  {
	  int auction; float predicted_increase;
	  float P_Cost = 0; float COST_Inflight = 0; float COST_Outflight = 0;
	  int inflight_auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_INFLIGHT, day_in);
	  if (items_available[inflight_auction] > 0)
	  {
		COST_Inflight = 0;
	  } else {
		  COST_Inflight = agent.getQuote(inflight_auction).getAskPrice();
	  }
	  
	  int outflight_auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_OUTFLIGHT, day_out);
	  if (items_available[outflight_auction] > 0)
	  {
		COST_Outflight = 0;
	  } else {
		  COST_Outflight = P_Cost += agent.getQuote(outflight_auction).getAskPrice();
	  }
	  
	  float COST_Hotel = 0;
	  for (int i = day_in; i < day_out; i++)
	  {
		  auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, hotel_type, i);
		  int otherHotel = agent.getAuctionFor(TACAgent.CAT_HOTEL,otherHotelType(hotel_type),i);
		  Quote q = agent.getQuote(auction);
	      	  Quote q2 = agent.getQuote(otherHotel);
		  if (items_available[auction] > 0)
		  {
			continue;
		  } else if (q.isAuctionClosed()) { COST_Hotel += 99999; }
		  COST_Hotel +=  predictHotelCost(q.getAskPrice(),q.getBidPrice(),q2.isAuctionClosed());
	  }
	  P_Cost = COST_Inflight + COST_Outflight + COST_Hotel;
	  return (int)P_Cost;
  }

  //TODO: Scale predicted cost as time goes by
  private float predicted_increase_time_period = 7.5f;
  private float predicted_increase_multiplier = 1.2f;
  private float predicted_increase_multiplier_other_closed = 1.4f;
  private float predictHotelCost(float currentAskPrice, float currentBidPrice, boolean other_closed)
  {
	float multiplier;
	if (other_closed) { multiplier = predicted_increase_multiplier_other_closed; } else { multiplier = predicted_increase_multiplier; }
	multiplier += 0.025*(8-n_hotels_closed);
	return ((8-n_hotels_closed) * predicted_increase_time_period) + (currentAskPrice * multiplier);
  }
  
  private int otherHotelType(int hotelType)
  {
	  if (hotelType == TACAgent.TYPE_CHEAP_HOTEL)
	  {
		  return TACAgent.TYPE_GOOD_HOTEL;
	  } else {
		  return TACAgent.TYPE_CHEAP_HOTEL;
	  }
  }
  
  private void temporary_package(int client,int day_in, int day_out, int hotel_type)
  {
	  	temporary_allocations[client][0] = day_in;
		temporary_allocations[client][1] = day_out;
		temporary_allocations[client][2] = hotel_type;

		int auction;

		for (int day = day_in; day < day_out; day++)
	{
		auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, hotel_type, day);
		if (items_available[auction] > 0)
		{
			items_available[auction] -= 1;
		}
		if (hqw_available[auction] > 0) 
		{
			hqw_available[auction] -= 1;
		}
	}
	auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_INFLIGHT,day_in);
	if (items_available[auction] > 0) { items_available[auction] -= 1; }

	auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_OUTFLIGHT,day_out);
	if (items_available[auction] > 0) { items_available[auction] -= 1; }
	  	
  }

  private void reset_items_available()
{
	items_available = new int[28];
	hqw_available = new int[28];
	 for (int i = 0; i < agent.getAuctionNo(); i++)
    {
	items_available[i] = agent.getOwn(i);
    }
	for (int i = 8; i < 16; i++)
	{
		hqw_available[i] = agent.getQuote(i).getHQW();
	}
}

  private int[][] temporary_allocations;
  private int[][] current_allocations;
  private int change_cost = 500;
  private void calculateAllocation() {
    temporary_allocations = new int[8][3];
    reset_items_available();
    
    for (int i = 0; i < 8; i++) {
	      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
	      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
	      int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
	      int type;

	      // Get the flight preferences auction and remember that we are
	      // going to buy tickets for these days. (inflight=1, outflight=0)
	      int current_best = -999999;
	      int this_round_c = 0;
	      int this_round_e = 0;
	      int best_type; int best_this_round;
	      int current_best_in = 0;
	      int current_best_out = 0; int current_best_type = 0;
	      for (int in = 1; in <= 4; in++)
	      {
	    	  for (int out = in + 1; out <= 5; out++)
	    	  {
	    		  this_round_c = predictUtility(i,in,out,TACAgent.TYPE_CHEAP_HOTEL);
	    		  this_round_e = predictUtility(i,in,out,TACAgent.TYPE_GOOD_HOTEL);
	    		  if (this_round_c > this_round_e)
	    		  {
	    			  best_type = TACAgent.TYPE_CHEAP_HOTEL; best_this_round = this_round_c;
	    		  } else { best_type = TACAgent.TYPE_GOOD_HOTEL; best_this_round = this_round_e; }
	    		  
	    		  if (best_this_round > current_best)
	    		  {
	    			  current_best = best_this_round;
	    			  current_best_in = in;
	    			  current_best_out = out;
	    			  current_best_type = best_type;
	    		  }
	    	  }
		}
	      temporary_package(i,current_best_in,current_best_out,current_best_type);
	}
      log.info(Arrays.deepToString(current_allocations));
      log.info(Arrays.deepToString(temporary_allocations));
    reset_items_available();
      int temp_util = get_util(temporary_allocations);
    reset_items_available();
      int current_util = get_util(current_allocations);
      log.info("Current Util: "+current_util+" ----- New util: "+temp_util+" ---- Change cost: "+change_cost);
      if (current_util + change_cost > temp_util) { log.info("No change in strategy."); return; } else {
	log.info("Altering strategy.");
	agent.clearAllocation();
		for (int client = 0; client < 8; client++) {
			allocate_package(client,temporary_allocations[client][0],temporary_allocations[client][1],temporary_allocations[client][2]);
		}
		current_allocations = temporary_allocations;
	}
	entertainmentAllocation();

  }
private int buy_price = 70;
private int min_buy = 85;
private int sell_price = 80;
private void entertainmentAllocation() {
	for (int client = 0; client < 8; client++) {
		int client_vals[] = new int[7];
		client_vals[TACAgent.TYPE_ALLIGATOR_WRESTLING] = agent.getClientPreference(client,TACAgent.TYPE_ALLIGATOR_WRESTLING);
		client_vals[TACAgent.TYPE_AMUSEMENT] = agent.getClientPreference(client,TACAgent.TYPE_AMUSEMENT);
		client_vals[TACAgent.TYPE_MUSEUM] = agent.getClientPreference(client,TACAgent.TYPE_MUSEUM);
		for (int d=current_allocations[client][0]; d < current_allocations[client][1]; d++)
		{

			int best = getBestEnt(client_vals);
			if (client_vals[best] <= min_buy) { break; }
			int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,best,d);
			agent.setAllocation(auction,agent.getAllocation(auction)+1);
			client_vals[best] = 0;
		}
	}
}

private int getBestEnt(int[] client_vals)
{
	if ((client_vals[TACAgent.TYPE_ALLIGATOR_WRESTLING] > client_vals[TACAgent.TYPE_AMUSEMENT]) && (client_vals[TACAgent.TYPE_ALLIGATOR_WRESTLING] > client_vals[TACAgent.TYPE_MUSEUM])) {
		return TACAgent.TYPE_ALLIGATOR_WRESTLING;
	} else if ((client_vals[TACAgent.TYPE_AMUSEMENT] > client_vals[TACAgent.TYPE_MUSEUM])) {
		return TACAgent.TYPE_AMUSEMENT;
	} else { return TACAgent.TYPE_MUSEUM; }
}

private void allocate_package(int client,int day_in, int day_out, int hotel_type)
{
	int auction;
	for (int day = day_in; day < day_out; day++)
	{
		auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, hotel_type, day);
		agent.setAllocation(auction, agent.getAllocation(auction) + 1);
		if (items_available[auction] > 0)
		{
			items_available[auction] -= 1;
		}
		if (hqw_available[auction] > 0) 
		{
			hqw_available[auction] -= 1;
		}
	}
	client_days[client][0] = day_in;
	client_days[client][1] = day_out;
	auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_INFLIGHT,day_in);
	if (items_available[auction] > 0) { items_available[auction] -= 1; }
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);

	auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_OUTFLIGHT,day_out);
	if (items_available[auction] > 0) { items_available[auction] -= 1; }
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);
}
  private float HQW_abandon_cost = 0.65f;
  private int get_util(int[][] allocations)
  {
	if (allocations[0][0] == 0) { return 0; }
	int retVal = 0; int HQWWaste = 0;
	for (int i = 0; i < 8; i++)
	{
		retVal += predictUtility(i,allocations[i][0],allocations[i][1],allocations[i][2]);
		applyAvailable(allocations[i]);
	}
	for (int i = 8; i < 16; i++) {
		if (hqw_available[i] > 0) { HQWWaste += hqw_available[i] * agent.getQuote(i).getBidPrice() * HQW_abandon_cost; }
	}
	log.fine("Raw Util: "+retVal+", HQW Wasted: "+HQWWaste+", Total Util: "+(retVal-HQWWaste));
	return retVal-HQWWaste;
  }

  private void applyAvailable(int[] allocations) {
	int auction;
	for (int day = allocations[0]; day < allocations[1]; day++) {
		auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, allocations[2], day);
		if (hqw_available[auction] > 0) { hqw_available[auction] -= 1; }
		if (items_available[auction] > 0) { items_available[auction] -= 1; }
	}
	}

  private int nextEntType(int client, int lastType) {
    int e1 = agent.getClientPreference(client, TACAgent.TYPE_ALLIGATOR_WRESTLING);
    int e2 = agent.getClientPreference(client, TACAgent.TYPE_AMUSEMENT);
    int e3 = agent.getClientPreference(client, TACAgent.TYPE_MUSEUM);

    // At least buy what each agent wants the most!!!
    if ((e1 > e2) && (e1 > e3) && lastType == -1)
      return TACAgent.TYPE_ALLIGATOR_WRESTLING;
    if ((e2 > e1) && (e2 > e3) && lastType == -1)
      return TACAgent.TYPE_AMUSEMENT;
    if ((e3 > e1) && (e3 > e2) && lastType == -1)
      return TACAgent.TYPE_MUSEUM;
    return -1;
  }



  // -------------------------------------------------------------------
  // Only for backward compability
  // -------------------------------------------------------------------

  public static void main (String[] args) {
    TACAgent.main(args);
  }

} // DummyAgent
