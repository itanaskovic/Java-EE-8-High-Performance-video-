package collections.compare.demo.cards.sortedset;

import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;

import collections.compare.demo.cards.Card;
import collections.compare.demo.cards.Rank;
import collections.compare.demo.cards.Suit;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeSet;

public class VavrDeckOfCards
{
    private SortedSet<Card> cards;
    private Map<Suit, ? extends SortedSet<Card>> cardsBySuit;

    public VavrDeckOfCards()
    {
        this.cards = Card.streamCards()
                .collect(TreeSet.collector());
        this.cardsBySuit = this.cards.groupBy(Card::getSuit);
    }

    public List<Card> shuffle(Random random)
    {
        // Unfortunately shuffle does not take Random as a parameter
        // so can't be easily tested
        //        return this.cards.toList()
        //                .shuffle()
        //                .shuffle()
        //                .shuffle().toStack();
        java.util.List<Card> shuffled = this.cards.toJavaList();
        Collections.shuffle(shuffled, random);
        Collections.shuffle(shuffled, random);
        Collections.shuffle(shuffled, random);
        return List.<Card>empty().pushAll(shuffled);
    }

    public Tuple2<Set<Card>, ? extends List<Card>> deal(List<Card> stack, int count)
    {
        Set<Card> hand = HashSet.empty();
        for (int i = 0; i < count; i++)
        {
            Tuple2<Card, ? extends List<Card>> cardTuple2 = stack.pop2();
            stack = cardTuple2._2();
            hand = hand.add(cardTuple2._1());
        }
        return Tuple.of(hand, stack);
    }

    public Card dealOneCard(List<Card> stack)
    {
        Tuple2<Card, ? extends List<Card>> cardTuple2 = stack.pop2();
        stack = cardTuple2._2();
        return cardTuple2._1();
    }

    public List<Set<Card>> shuffleAndDeal(Random random, int hands, int cardsPerHand)
    {
        List<Card> shuffled = this.shuffle(random);
        return this.dealHands(shuffled, hands, cardsPerHand);
    }

    public List<Set<Card>> dealHands(
            List<Card> shuffled,
            int hands,
            int cardsPerHand)
    {
        List<Set<Card>> list = List.empty();
        for (int i = 0; i < hands; i++)
        {
            Tuple2<Set<Card>, ? extends List<Card>> tuple2 =
                    this.deal(shuffled, cardsPerHand);
            shuffled = tuple2._2();
            list = list.append(tuple2._1());
        }
        return list;
    }

    public SortedSet<Card> diamonds()
    {
        return this.cardsBySuit.get(Suit.DIAMONDS).get();
    }

    public SortedSet<Card> hearts()
    {
        return this.cardsBySuit.get(Suit.HEARTS).get();
    }

    public SortedSet<Card> spades()
    {
        return this.cardsBySuit.get(Suit.SPADES).get();
    }

    public SortedSet<Card> clubs()
    {
        return this.cardsBySuit.get(Suit.CLUBS).get();
    }

    public java.util.Map<Suit, Long> countsBySuit()
    {
        return this.cards.collect(
                Collectors.groupingBy(
                        Card::getSuit,
                        Collectors.counting()));
    }

    public java.util.Map<Rank, Long> countsByRank()
    {
        return this.cards.collect(
                Collectors.groupingBy(
                        Card::getRank,
                        Collectors.counting()));
    }

    public SortedSet<Card> getCards()
    {
        return this.cards;
    }

    public Map<Suit, ? extends SortedSet<Card>> getCardsBySuit()
    {
        return this.cardsBySuit;
    }
}
