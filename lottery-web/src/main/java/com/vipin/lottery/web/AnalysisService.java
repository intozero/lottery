package com.vipin.lottery.web;

import org.springframework.stereotype.Service;
import totsincecombined.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    public Map<String,Object> analyze(List<DrawRecord> draws,String game) {
        Statistics stats=new Statistics(DrawRecord.maximum(game));
        List<Map<String,Object>> sums=new ArrayList<>();
        Map<Integer,Integer> sumCounts=new TreeMap<>(), deviationCounts=new TreeMap<>(), first=new TreeMap<>(), last=new TreeMap<>(), ends=new TreeMap<>();
        Map<String,List<String>> combinations=new LinkedHashMap<>();
        long total=0;
        for(DrawRecord d:draws) {
            stats.accept(new Draw(d.date(),d.whites().stream().mapToInt(Integer::intValue).toArray()));
            total+=d.sum();
            sums.add(Map.of("date",d.date(),"whites",d.whites(),"sum",d.sum(),"mean",d.sum()/5.0,"deviation",d.deviation(),
                    "runningTotal",total,"runningAverage",(double)total/sumsSize(sums)));
            sumCounts.merge(d.sum(),1,Integer::sum);
            deviationCounts.merge((int)Math.floor(d.deviation()),1,Integer::sum);
            first.merge(d.whites().get(0),1,Integer::sum);last.merge(d.whites().get(4),1,Integer::sum);
            ends.merge(d.whites().get(0)+d.whites().get(4),1,Integer::sum);
            combinations.computeIfAbsent(d.values(),k->new ArrayList<>()).add(d.date().toString());
        }
        List<Map<String,Object>> ranges=new ArrayList<>();
        var numbers=stats.numbers();
        for(int i=0;i<=DrawRecord.maximum(game)/10;i++) {
            final int bucket=i;
            ranges.add(Map.of("range",stats.rangeLabel(i),"total",numbers.stream().filter(n->n.getNumber()/10==bucket).mapToLong(NumberStats::getTotal).sum(),
                    "since",numbers.stream().filter(n->n.getNumber()/10==bucket).mapToLong(NumberStats::getSince).sum()));
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("count",draws.size());result.put("latest",draws.isEmpty()?null:draws.get(draws.size()-1));
        result.put("totalSum",total);result.put("averageSum",draws.isEmpty()?0:(double)total/draws.size());
        result.put("numbers",numbers);result.put("sums",sums);result.put("sumCounts",sumCounts);
        result.put("deviations",deviationCounts);result.put("first",first);result.put("last",last);result.put("endSums",ends);
        result.put("ranges",ranges);result.put("patterns",stats.patterns());result.put("shapes",stats.shapes());result.put("occupancies",stats.occupancies());
        result.put("repeated",combinations.entrySet().stream().filter(e->e.getValue().size()>1).map(e->Map.of("balls",e.getKey(),"dates",e.getValue())).toList());
        return result;
    }
    private int sumsSize(List<?> sums) { return sums.size()+1; }

    public Map<String,Object> digits(List<DrawRecord> draws,int window) {
        if(window<1||window>100) throw new IllegalArgumentException("Window must be 1–100");
        if(draws.stream().anyMatch(d->d.special()==null)) throw new IllegalArgumentException("Digit analysis requires special balls for every selected draw");
        String digits=draws.stream().map(d->d.values().replace(" ","")).collect(Collectors.joining());
        if(window>digits.length()) throw new IllegalArgumentException("Window exceeds selected digit-stream length");
        Map<String,Integer> counts=new HashMap<>();
        for(int i=0;i<=digits.length()-window;i++) counts.merge(digits.substring(i,i+window),1,Integer::sum);
        var sorted=counts.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList();
        return Map.of("digits",digits.length(),"windows",digits.length()-window+1,"unique",counts.size(),
                "rows",sorted.stream().limit(1000).map(e->Map.of("pattern",e.getKey(),"count",e.getValue())).toList(),"truncated",sorted.size()>1000);
    }

    public List<Map<String,Object>> timeline(List<DrawRecord> draws,int number,String game) {
        if(number<1||number>DrawRecord.maximum(game)) throw new IllegalArgumentException("Number out of range");
        List<Map<String,Object>> result=new ArrayList<>();int previous=0,total=0;
        for(int i=0;i<draws.size();i++) {
            DrawRecord d=draws.get(i);boolean hit=d.whites().contains(number);
            if(hit) {total++;previous=i+1;}
            result.add(Map.of("date",d.date(),"draw",i+1,"appeared",hit,"total",total,"since",i+1-previous));
        }
        return result;
    }

    public Map<String,Object> combinations(int maximum,int sum,Integer deviation) {
        if(maximum<5||maximum>75||sum<15||sum>5*maximum-10||deviation!=null&&(deviation<0||deviation>40))
            throw new IllegalArgumentException("Choose maximum 5–75, a feasible sum, and deviation 0–40");
        List<List<Integer>> rows=new ArrayList<>();
        search(new ArrayList<>(),1,maximum,sum,deviation,rows);
        boolean truncated=rows.size()>500;
        return Map.of("rows",rows.subList(0,Math.min(500,rows.size())),"truncated",truncated);
    }
    private void search(List<Integer> chosen,int start,int maximum,int remaining,Integer deviation,List<List<Integer>> result) {
        if(result.size()>500)return;
        int left=5-chosen.size();
        if(left==0) {
            if(remaining==0) {
                double mean=chosen.stream().mapToInt(Integer::intValue).sum()/5.0;
                int sd=(int)Math.floor(Math.sqrt(chosen.stream().mapToDouble(n->(n-mean)*(n-mean)).sum()/5));
                if(deviation==null||deviation==sd)result.add(List.copyOf(chosen));
            }
            return;
        }
        if(start+left-1>maximum||remaining<left*start+left*(left-1)/2||remaining>left*maximum-left*(left-1)/2)return;
        for(int n=start;n<=maximum-left+1;n++) {
            chosen.add(n);search(chosen,n+1,maximum,remaining-n,deviation,result);chosen.remove(chosen.size()-1);
            if(result.size()>500)return;
        }
    }

    public List<Map<String,Object>> rangeUniverse(List<DrawRecord> draws,String game) {
        int max=DrawRecord.maximum(game);int[] capacity=new int[max/10+1];
        for(int n=1;n<=max;n++)capacity[n/10]++;
        Map<String,Integer> observed=new HashMap<>();
        for(DrawRecord d:draws) {
            int[] p=new int[capacity.length];d.whites().forEach(n->p[n/10]++);
            observed.merge(key(p),1,Integer::sum);
        }
        List<Map<String,Object>> result=new ArrayList<>();
        patterns(capacity,new int[capacity.length],0,5,observed,result);
        return result;
    }
    private void patterns(int[] capacity,int[] p,int at,int left,Map<String,Integer> observed,List<Map<String,Object>> result) {
        if(at==p.length) {
            if(left==0) {
                long ways=1;int square=0;
                for(int i=0;i<p.length;i++){ways*=choose(capacity[i],p[i]);square+=p[i]*p[i];}
                result.add(Map.of("pattern",key(p),"observed",observed.getOrDefault(key(p),0),"combinations",ways,"square",square));
            }
            return;
        }
        for(int n=0;n<=Math.min(capacity[at],left);n++){p[at]=n;patterns(capacity,p,at+1,left-n,observed,result);}
    }
    private String key(int[] p){return Arrays.stream(p).mapToObj(Integer::toString).collect(Collectors.joining("-"));}
    private long choose(int n,int k){long r=1;for(int i=1;i<=k;i++)r=r*(n-i+1)/i;return r;}
}
