import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DeadlockSimulator extends JFrame {
    private JTextArea logArea;
    private JButton btnAddProcess, btnAddResource, btnRequestResource;

    private List<Process> processos = new ArrayList<>();
    private List<Resource> recursos = new ArrayList<>();

    public DeadlockSimulator() {
        setTitle("Simulador de Deadlock e Prevenção");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel panel = new JPanel();

        btnAddProcess = new JButton("Adicionar Processo");
        btnAddResource = new JButton("Adicionar Recurso");
        btnRequestResource = new JButton("Solicitar Recurso");

        panel.add(btnAddProcess);
        panel.add(btnAddResource);
        panel.add(btnRequestResource);

        add(panel, BorderLayout.SOUTH);

        btnAddProcess.addActionListener(e -> {
            Process p = new Process("P" + (processos.size() + 1));
            processos.add(p);
            logArea.append("Processo criado: " + p.name + "\n");
        });

        btnAddResource.addActionListener(e -> {
            Resource r = new Resource("R" + (recursos.size() + 1));
            recursos.add(r);
            logArea.append("Recurso criado: " + r.name + "\n");
        });

        btnRequestResource.addActionListener(e -> {
            if (processos.isEmpty() || recursos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Crie pelo menos 1 processo e 1 recurso antes.");
                return;
            }

            String[] procNames = processos.stream().map(p -> p.name).toArray(String[]::new);
            String procName = (String) JOptionPane.showInputDialog(this, "Escolha um processo:", "Selecionar Processo",
                    JOptionPane.PLAIN_MESSAGE, null, procNames, procNames[0]);

            String[] resNames = recursos.stream().map(r -> r.name).toArray(String[]::new);
            String resName = (String) JOptionPane.showInputDialog(this, "Escolha um recurso:", "Selecionar Recurso",
                    JOptionPane.PLAIN_MESSAGE, null, resNames, resNames[0]);

            if (procName == null || resName == null) return;

            Process p = processos.stream().filter(x -> x.name.equals(procName)).findFirst().get();
            Resource r = recursos.stream().filter(x -> x.name.equals(resName)).findFirst().get();

            boolean granted = tryAllocateResource(p, r);

            if (granted) {
                logArea.append("Recurso " + r.name + " alocado para processo " + p.name + "\n");
            } else {
                logArea.append("Recurso " + r.name + " NÃO pode ser alocado para processo " + p.name + " (prevenção de deadlock)\n");
            }
        });
    }

    private boolean tryAllocateResource(Process p, Resource r) {
        if (r.allocatedTo != null) {
            p.waitingFor = r;
            return false;
        }

        r.allocatedTo = p;
        p.resourcesHeld.add(r);
        p.waitingFor = null;

        if (detectDeadlock()) {
            r.allocatedTo = null;
            p.resourcesHeld.remove(r);
            p.waitingFor = r;
            return false;
        }

        return true;
    }

    private boolean detectDeadlock() {
        Map<Process, List<Process>> graph = new HashMap<>();
        for (Process p : processos) {
            graph.put(p, new ArrayList<>());
        }

        for (Process p : processos) {
            if (p.waitingFor != null && p.waitingFor.allocatedTo != null) {
                Process holder = p.waitingFor.allocatedTo;
                graph.get(p).add(holder);
            }
        }

        Set<Process> visited = new HashSet<>();
        Set<Process> recursionStack = new HashSet<>();

        for (Process p : processos) {
            if (detectCycleDFS(p, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCycleDFS(Process current, Map<Process, List<Process>> graph,
                                   Set<Process> visited, Set<Process> recursionStack) {
        if (recursionStack.contains(current)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);
        recursionStack.add(current);

        for (Process neighbor : graph.get(current)) {
            if (detectCycleDFS(neighbor, graph, visited, recursionStack)) return true;
        }

        recursionStack.remove(current);
        return false;
    }

    // Classes auxiliares

    static class Process {
        String name;
        List<Resource> resourcesHeld = new ArrayList<>();
        Resource waitingFor = null;

        Process(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class Resource {
        String name;
        Process allocatedTo = null;

        Resource(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DeadlockSimulator sim = new DeadlockSimulator();
            sim.setVisible(true);
        });
    }
}