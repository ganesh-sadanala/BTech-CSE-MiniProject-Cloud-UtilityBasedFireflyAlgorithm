import java.util.*;
import java.io.*;

class work {
  public static void main(String[] args) {
    Scanner inp = new Scanner(System.in);
    int a, b;
    System.out.print("Enter number of jobs: ");
    a = inp.nextInt();
    System.out.print("Enter number of virtual machines: ");
    b = inp.nextInt();
    jobnode[] jobs = new jobnode[a];
    vmnode[] vms = new vmnode[b];
    Random rand = new Random();
    for (int i = 0; i < a; i++) {
      int pow = rand.nextInt(200);
      int mem = rand.nextInt(5000);
      int bw = rand.nextInt(2000);
      int loc = rand.nextInt(50000);
      jobs[i] = new jobnode(pow, mem, bw, loc);
    }
    for (int i = 0; i < b; i++) {
      int pow = rand.nextInt(200) + 400;
      int mem = rand.nextInt(5000) + 10000;
      int bw = rand.nextInt(2000) + 5000;
      int speed = rand.nextInt(50000);
      vms[i] = new vmnode(pow, mem, bw, speed);
    }

    // calculate the ECT
    double[][] ect = new double[a][b];
    for (int i = 0; i < a; i++) {
      for (int j = 0; j < b; j++) {
        ect[i][j] = jobs[i].loc * 1.0 / vms[j].prospeed;
      }
    }

    System.out.print("Enter number of iterations: ");
    int n = inp.nextInt();
    HashMap<Integer, Integer> res = new HashMap<>();
    double best = Double.MAX_VALUE;
    int[] assigned = new int[a];
    Arrays.fill(assigned, -1);
    /*
     * for(int j=0;j<10;j++) { update(jobs[j],j,j/2,assigned[j],vms,res);
     * assigned[j]=j/2; res.put(j,j/2); }
     */
    for (int x = 0; x < n; x++) {
      for (int j = 0; j < a; j++) {
        // int ind=res.get(j);
        int ind = assigned[j];
        double lv = Double.MAX_VALUE;
        for (int v = 0; v < b; v++) {
          if (v == assigned[j])
            continue;
          double temp = lv;
          if (jobs[j].mem <= vms[v].memavail)
            temp = loadValue(jobs[j], vms[v]);
          if (temp < lv) {
            lv = temp;
            ind = v;
          }
        }
        update(jobs[j], j, ind, assigned[j], vms, res);
        assigned[j] = ind;
        // System.out.println(x+" "+j+" "+ind);
      }
      double factor = fitness(vms, jobs, assigned, res, ect);
      // System.out.println(factor);
      if (factor < best) {
        best = factor;
        for (int i = 0; i < a; i++)
          res.put(i, assigned[i]);
      }
      // System.out.println(best+" "+res);
    }

    display(best, res);
  }

  // calculates the reliability of a VM
  static double reliability(vmnode vm, jobnode[] jobs, int[] assigned, int ind) {
    double pow, bw, mem;
    pow = bw = mem = 0;

    for (int i = 0; i < jobs.length; i++) {
      if (assigned[i] == ind) {
        pow = pow + 1.0 * jobs[i].pow;
        bw = bw + 1.0 * jobs[i].bw;
      }
    }
    pow = 0.5 * (vm.powavail * 1.0 / pow);
    bw = 0.3 * (vm.bwavail * 1.0 / bw);
    mem = 0.2 * (vm.memavail + vm.memused) / vm.memused;
    if (vm.memused == 0.0) {
      return 1000000.0;
    }
    return (pow + bw + mem);
  }

  // calculates the Load Imbalance Degree
  static double LIBD(vmnode[] vms, jobnode[] jobs, int[] assigned) {
    double rel[] = new double[vms.length];
    double avg = 0, result = 0;
    for (int i = 0; i < vms.length; i++) {
      rel[i] = reliability(vms[i], jobs, assigned, i);
      avg += rel[i];
    }
    avg /= vms.length;
    for (int i = 0; i < vms.length; i++) {
      result += ((rel[i] - avg) * (rel[i] - avg));
    }
    return result;
  }

  static void update(jobnode job, int k, int ind, int prev, vmnode[] vms, HashMap<Integer, Integer> map) {
    vms[ind].memavail -= job.mem;
    vms[ind].memused += job.mem;
    map.put(k, ind);
    if (prev == -1)
      return;
    vms[prev].memavail += job.mem;
    vms[prev].memused -= job.mem;
  }

  static double loadValue(jobnode job, vmnode vm) {
    // System.out.println("here");
    double pow, mem, bw;
    pow = job.pow * 1.0 / vm.powavail;
    bw = job.bw * 1.0 / vm.bwavail;
    mem = job.mem * 1.0 / vm.memavail;
    // System.out.println(pow+" "+mem+" "+bw);
    if (mem >= 1)
      return Double.MAX_VALUE;
    return 0.5 * pow + 0.2 * mem + 0.3 * bw;
  }

  static void display(double best, HashMap<Integer, Integer> res) {
    System.out.println("fitness value " + best + "\n job mapped to vm");
    for (int i = 0; i < res.size(); i++) {
      System.out.println(i + " -> " + res.get(i));
    }
  }

  // calculates the fitness value
  static double fitness(vmnode[] vms, jobnode[] jobs, int[] assigned, HashMap<Integer, Integer> map, double[][] ect) {
    double libd = LIBD(vms, jobs, assigned);
    double makesp = calcmakespan(map, ect);
    return libd * makesp;
  }

  // calculates the Makespan
  static double calcmakespan(HashMap<Integer, Integer> mapping, double[][] ect) {

    HashMap<Integer, Double> vm = new HashMap<>();
    double max = -1;
    for (Integer i : mapping.keySet()) {
      int currvm = mapping.get(i);
      vm.put(currvm, vm.getOrDefault(currvm, 0.0) + ect[i][currvm]);
      max = Math.max(max, vm.get(currvm));
    }
    return max;

  }
}

class jobnode {
  int pow;
  int mem;
  int bw;
  int loc;

  jobnode(int pow, int mem, int bw, int loc) {
    this.pow = pow;
    this.mem = mem;
    this.bw = bw;
    this.loc = loc;
  }
}

class vmnode {
  int powavail;
  int memavail;
  int bwavail;
  int memused;
  int prospeed;

  vmnode(int pow, int mem, int bw, int prospeed) {
    powavail = pow;
    memavail = mem;
    bwavail = bw;
    memused = 0;
    this.prospeed = prospeed;
  }
}
