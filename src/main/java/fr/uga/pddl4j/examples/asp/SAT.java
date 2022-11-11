package fr.uga.pddl4j.examples.asp;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.PlannerConfiguration;
import fr.uga.pddl4j.planners.SearchStrategy;
import fr.uga.pddl4j.planners.statespace.search.StateSpaceSearch;
import fr.uga.pddl4j.problem.*;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Condition;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.problem.operator.Effect;
import fr.uga.pddl4j.util.BitVector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import picocli.CommandLine;

import java.util.*;

/**
 * The class is an example. It shows how to create a simple A* search planner able to
 * solve an ADL problem by choosing the heuristic to used and its weight.
 *
 * @author Nicolas Girardier
 * @version 4.0 - 17/10/2022
 */
@CommandLine.Command(name = "ASP",
    version = "ASP 1.0",
    description = "Solves a specified planning problem using A* search strategy.",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    headerHeading = "Usage:%n",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n")


public class SAT extends AbstractPlanner {

    /**
     * The HEURISTIC property used for planner configuration.
     */
    public static final String HEURISTIC_SETTING = "HEURISTIC";

    /**
     * The default value of the HEURISTIC property used for planner configuration.
     */
    public static final StateHeuristic.Name DEFAULT_HEURISTIC = StateHeuristic.Name.FAST_FORWARD;

    /**
     * The WEIGHT_HEURISTIC property used for planner configuration.
     */
    public static final String WEIGHT_HEURISTIC_SETTING = "WEIGHT_HEURISTIC";

    /**
     * The default value of the WEIGHT_HEURISTIC property used for planner configuration.
     */
    public static final double DEFAULT_WEIGHT_HEURISTIC = 1.0;

    /**
     * The weight of the heuristic.
     */
    private double heuristicWeight;

    /**
     * The name of the heuristic used by the planner.
     */
    public StateHeuristic.Name heuristic;

    /**
     * Returns the configuration of the planner.
     *
     * @return the configuration of the planner.
     */
    @Override
    public PlannerConfiguration getConfiguration() {
        final PlannerConfiguration config = super.getConfiguration();
        config.setProperty(ASP.HEURISTIC_SETTING, this.getHeuristic().toString());
        config.setProperty(ASP.WEIGHT_HEURISTIC_SETTING, Double.toString(this.getHeuristicWeight()));
        return config;
    }

    /**
     * Sets the configuration of the planner. If a planner setting is not defined in
     * the specified configuration, the setting is initialized with its default value.
     *
     * @param configuration the configuration to set.
     */
    @Override
    public void setConfiguration(final PlannerConfiguration configuration) {
        super.setConfiguration(configuration);
        if (configuration.getProperty(ASP.WEIGHT_HEURISTIC_SETTING) == null) {
            this.setHeuristicWeight(ASP.DEFAULT_WEIGHT_HEURISTIC);
        } else {
            this.setHeuristicWeight(Double.parseDouble(configuration.getProperty(
                ASP.WEIGHT_HEURISTIC_SETTING)));
        }
        if (configuration.getProperty(ASP.HEURISTIC_SETTING) == null) {
            this.setHeuristic(ASP.DEFAULT_HEURISTIC);
        } else {
            this.setHeuristic(StateHeuristic.Name.valueOf(configuration.getProperty(
                ASP.HEURISTIC_SETTING)));
        }
    }

    /**
     * This method return the default arguments of the planner.
     *
     * @return the default arguments of the planner.
     * @see PlannerConfiguration
     */
    public static PlannerConfiguration getDefaultConfiguration() {
        PlannerConfiguration config = Planner.getDefaultConfiguration();
        config.setProperty(ASP.HEURISTIC_SETTING, ASP.DEFAULT_HEURISTIC.toString());
        config.setProperty(ASP.WEIGHT_HEURISTIC_SETTING,
            Double.toString(ASP.DEFAULT_WEIGHT_HEURISTIC));
        return config;
    }


    /**
     * Checks the planner configuration and returns if the configuration is valid.
     * A configuration is valid if (1) the domain and the problem files exist and
     * can be read, (2) the timeout is greater than 0, (3) the weight of the
     * heuristic is greater than 0 and (4) the heuristic is a not null.
     *
     * @return <code>true</code> if the configuration is valid <code>false</code> otherwise.
     */
    public boolean hasValidConfiguration() {
        return super.hasValidConfiguration()
            && this.getHeuristicWeight() > 0.0
            && this.getHeuristic() != null;
    }

    /**
     * Sets the weight of the heuristic.
     *
     * @param weight the weight of the heuristic. The weight must be greater than 0.
     * @throws IllegalArgumentException if the weight is strictly less than 0.
     */
    @CommandLine.Option(names = {"-w", "--weight"}, defaultValue = "1.0",
        paramLabel = "<weight>", description = "Set the weight of the heuristic (preset 1.0).")
    public void setHeuristicWeight(final double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight <= 0");
        }
        this.heuristicWeight = weight;
    }

    /**
     * Set the name of heuristic used by the planner to the solve a planning problem.
     *
     * @param heuristic the name of the heuristic.
     */
    @CommandLine.Option(names = {"-e", "--heuristic"}, defaultValue = "FAST_FORWARD",
        description = "Set the heuristic : AJUSTED_SUM, AJUSTED_SUM2, AJUSTED_SUM2M, COMBO, "
            + "MAX, FAST_FORWARD SET_LEVEL, SUM, SUM_MUTEX (preset: FAST_FORWARD)")
    public void setHeuristic(StateHeuristic.Name heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Returns the name of the heuristic used by the planner to solve a planning problem.
     *
     * @return the name of the heuristic used by the planner to solve a planning problem.
     */
    public final StateHeuristic.Name getHeuristic() {
        return this.heuristic;
    }

    /**
     * Returns the weight of the heuristic.
     *
     * @return the weight of the heuristic.
     */
    public final double getHeuristicWeight() {
        return this.heuristicWeight;
    }

    /**
     * The class logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(ASP.class.getName());
    /**
     * Instantiates the planning problem from a parsed problem.
     *
     * @param problem the problem to instantiate.
     * @return the instantiated planning problem or null if the problem cannot be instantiated.
     */
    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    /**
     * Search a solution plan to a specified domain and problem using A*.
     *
     * @param problem the problem to solve.
     * @return the plan found or null if no plan was found.
     */
    @Override
    public Plan solve(final Problem problem) {


        final int MAXVAR = 1000000;
        final int NBCLAUSES = 500000;

        ISolver solver = SolverFactory.newDefault();

        // prepare the solver to accept MAXVAR variables. MANDATORY for MAXSAT solving
        solver.newVar(MAXVAR);
        solver.setExpectedNumberOfClauses(NBCLAUSES);
        // Feed the solver using Dimacs format, using arrays of int

        // On ajoute les actions et les fluents dans un dictionnaire
        List<Action> actions = problem.getActions();
        List<Fluent> fluents = problem.getFluents();
        TreeMap < Integer, Object > map= new TreeMap<>();

        int index = 1;

        for(Fluent fluent : fluents){
            map.put(index,fluent );
            index ++;
        }


        for(Action action : actions){
            map.put(index,action);
            index ++;
        }

        // Première clause pour l'état initial //////////////////////////////////////////////////////////////////////////////////
        List<Integer> clauseInitialState = new ArrayList<>();

        int []initialStateNegativeFluents = problem.getInitialState().getNegativeFluents().stream().toArray();
        int []initialStatePositiveFluents = problem.getInitialState().getPositiveFluents().stream().toArray();
        int []initialStateNegativeFluents2 = new int[initialStateNegativeFluents.length];

        // On ajoute les positive et negative fluents à la clause
        for(int i = 0; i < initialStateNegativeFluents.length; i++)
        {
            initialStateNegativeFluents2[i]=initialStateNegativeFluents[i]*-1;

            if(initialStateNegativeFluents2[i] !=0){
                clauseInitialState.add(initialStateNegativeFluents2[i]);
            }
        }

        for(int i = 0;i <initialStatePositiveFluents.length; i++)
        {
            if(initialStatePositiveFluents[i] !=0){
                clauseInitialState.add(initialStatePositiveFluents[i]*-1);
            }
        }

        int[] clauseInitialStateInt = clauseInitialState.stream().mapToInt(i ->1).toArray();
        try {
            solver.addClause(new VecInt(clauseInitialStateInt));
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }

        // Clause pour le goal state ////////////////////////////////////////////////////////////////////////////////

        List<Integer> clauseGoalState = new ArrayList<>();

        int []goalStateNegativeFluents = problem.getGoal().getNegativeFluents().stream().toArray();
        int []goalStatePositiveFluents = problem.getGoal().getPositiveFluents().stream().toArray();
        int []goalStateNegativeFluents2 = new int[goalStateNegativeFluents.length];


        for(int i = 0; i < goalStateNegativeFluents.length; i++)
        {
            goalStateNegativeFluents2[i]=goalStateNegativeFluents[i]*-1;

            if(goalStateNegativeFluents2[i] !=0){
                clauseGoalState.add(goalStateNegativeFluents2[i]);
            }
        }

        for(int i = 0;i <goalStatePositiveFluents.length; i++)
        {
            if(goalStatePositiveFluents[i] !=0){

                clauseGoalState.add(goalStatePositiveFluents[i]*-1);
            }
        }

        int[] clauseGoalStateInt = clauseGoalState.stream().mapToInt(i ->1).toArray();
        try {
            solver.addClause(new VecInt(clauseGoalStateInt));
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }

        ////////////////////////////// Parcours des actions et ajout des clauses
        for(Action action : actions){


            // Clause pour les preconditions
            // Récupération des preconditions de l'action
            List<Integer> clausePrecond = new ArrayList<>();
            Condition precond = action.getPrecondition();
            int[] precondPosFluents = precond.getPositiveFluents().stream().toArray();
            int[] precondNegFluents = precond.getNegativeFluents().stream().toArray();
            int[] precondNegFluents2 = new int[precondNegFluents.length];


            for(int i = 0;i <precondNegFluents.length; i++) // Fluents négatifs des preconditions de l'action
            {
                precondNegFluents2[i]=precondNegFluents[i]*-1;

                if(precondNegFluents2[i] !=0){
                    clausePrecond.add(precondNegFluents2[i]);
                }
            }

            for(int i = 0;i <precondPosFluents.length; i++) // Fluents positifs des préconditions de l'action
            {
                if(precondPosFluents[i] !=0){
                    clausePrecond.add(precondPosFluents[i]*-1);
                }
            }

            // Transformation de la Clause d'une liste à un tableau de int
            int[] clausePrecondInt = clausePrecond.stream().mapToInt(i ->1).toArray();
            try {
                // Ajout de la clause au solver
                solver.addClause(new VecInt(clausePrecondInt));
            } catch (ContradictionException e) {
                throw new RuntimeException(e);
            }

            ///////////////////////////////// On ajoute les clauses pour les effets de l'action

            // Récupération des effets de l'action
            List<ConditionalEffect> effects = new ArrayList<>();
            effects = action.getConditionalEffects();

            for(ConditionalEffect effect : effects) {
                List<Integer> effectClause = new ArrayList<>();

                int[] negativeFluents = effect.getEffect().getNegativeFluents().stream().toArray();
                int[] positiveFluents = effect.getEffect().getPositiveFluents().stream().toArray();

                int[] negativeFluents2 = new int[negativeFluents.length];

                //effectClause.add(actionNumber);

                for(int i = 0;i <negativeFluents.length; i++) // Fluents négatifs pour les effets de l'action
                {
                    negativeFluents2[i]=negativeFluents[i]*-1;

                    if(negativeFluents2[i] !=0){
                        effectClause.add(negativeFluents2[i]);
                    }
                }

                for(int i = 0;i <positiveFluents.length; i++) // Fluents positifs pour les effets de l'action
                {
                    if(positiveFluents[i] !=0){
                        effectClause.add(positiveFluents[i]*-1);
                    }
                }

                int[] effectClauseInt = effectClause.stream().mapToInt(i ->1).toArray();
                try {
                    solver.addClause(new VecInt(effectClauseInt));
                } catch (ContradictionException e) {
                    throw new RuntimeException(e);
                }
            }

        }


        // we are done. Working now on the IProblem interface
        IProblem problemia = solver;
        try {
            if (problemia.isSatisfiable()) {

                System.out.println("le problème est satisfiable");

                for(int num : solver.model()){
                    System.out.println(num);
                    //on récupère les fluents et les actions du dictionnaire

                }
            } else {
                System.out.println("Le problème n'est pas satisfiable");
            }
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Extracts a search from a specified node.
     *
     * @param node    the node.
     * @param problem the problem.
     * @return the search extracted from the specified node.
     */
    private Plan extractPlan(final Node node, final Problem problem) {
        Node n = node;
        final Plan plan = new SequentialPlan();
        while (n.getAction() != -1) {
            final Action a = problem.getActions().get(n.getAction());
            plan.add(0, a);
            n = n.getParent();
        }
        return plan;
    }

    /**
     * Search a solution plan for a planning problem using an A* search strategy.
     *
     * @param problem the problem to solve.
     * @return a plan solution for the problem or null if there is no solution
     */
    public Plan astar(Problem problem) {

        // First we create an instance of the heuristic to use to guide the search
        final StateHeuristic heuristic = StateHeuristic.getInstance(this.getHeuristic(), problem);

        // We get the initial state from the planning problem
        final State init = new State(problem.getInitialState());

        // We initialize the closed list of nodes (store the nodes explored)
        final Set<Node> close = new HashSet<>();

        // We initialize the opened list to store the pending node according to function f
        final double weight = this.getHeuristicWeight();
        final PriorityQueue<Node> open = new PriorityQueue<>(100, new Comparator<Node>() {
            public int compare(Node n1, Node n2) {
                double f1 = weight * n1.getHeuristic() + n1.getCost();
                double f2 = weight * n2.getHeuristic() + n2.getCost();
                return Double.compare(f1, f2);
            }
        });

        // We create the root node of the tree search
        final Node root = new Node(init, null, -1, 0, heuristic.estimate(init, problem.getGoal()));

        // We add the root to the list of pending nodes
        open.add(root);
        Plan plan = null;

        // We set the timeout in ms allocated to the search
        final int timeout = this.getTimeout() * 1000;
        long time = 0;

        // We start the search
        while (!open.isEmpty() && plan == null && time < timeout) {

            // We pop the first node in the pending list open
            final Node current = open.poll();
            close.add(current);

            // If the goal is satisfied in the current node then extract the search and return it
            if (current.satisfy(problem.getGoal())) {
                return this.extractPlan(current, problem);
            } else { // Else we try to apply the actions of the problem to the current node
                for (int i = 0; i < problem.getActions().size(); i++) {
                    // We get the actions of the problem
                    Action a = problem.getActions().get(i);
                    // If the action is applicable in the current node
                    if (a.isApplicable(current)) {
                        Node next = new Node(current);
                        // We apply the effect of the action
                        final List<ConditionalEffect> effects = a.getConditionalEffects();
                        for (ConditionalEffect ce : effects) {
                            if (current.satisfy(ce.getCondition())) {
                                next.apply(ce.getEffect());
                            }
                        }
                        // We set the new child node information
                        final double g = current.getCost() + 1;
                        if (!close.contains(next)) {
                            next.setCost(g);
                            next.setParent(current);
                            next.setAction(i);
                            next.setHeuristic(heuristic.estimate(next, problem.getGoal()));
                            open.add(next);
                        }
                    }
                }
            }
        }

        // Finally, we return the search computed or null if no search was found
        return plan;
    }

    /**
     * Creates a new A* search planner with the default configuration.
     */
    public SAT() {
        this(ASP.getDefaultConfiguration());
    }

    /**
     * Creates a new A* search planner with a specified configuration.
     *
     * @param configuration the configuration of the planner.
     */
    public SAT(final PlannerConfiguration configuration) {
        super();
        this.setConfiguration(configuration);
    }


    /**
     * The main method of the <code>ASP</code> planner.
     *
     * @param args the arguments of the command line.
     */
    public static void main(String[] args) {
        try {
            final SAT planner = new SAT();
            CommandLine cmd = new CommandLine(planner);
            cmd.execute(args);
        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }
    }

}

