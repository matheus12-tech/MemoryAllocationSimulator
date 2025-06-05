import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SimuladorDeAlocacaoDeMemoria extends JFrame {
    private final JComboBox<String> opcoesEstrategia;
    private final DefaultListModel<String> modeloListaDeProcessos;
    private final List<BlocoDeMemoria> blocosDeMemoria;
    private final List<Processo> processos;
    private final JPanel painelDeMemoria;
    private final JButton botaoDeAlocacao, botaoReset, botaoSimularES; //IO = input output, buzeta
    private final JTextField campoNome, campoTamanho;
    private final JLabel rotuloStatusMemoria;
    private int proximoIndiceDeEncaixe = 0;

    private SimuladorDeAlocacaoDeMemoria() {
        super("Simulador de Alocação de Memória");
        setLayout(new BorderLayout());

        opcoesEstrategia = new JComboBox<>(new String[]{
                "primeiro encaixe", "melhor encaixe", "pior encaixe", "próximo encaixe"
        });

        modeloListaDeProcessos = new DefaultListModel<>();
        blocosDeMemoria = new ArrayList<>(Arrays.asList(
                new BlocoDeMemoria(0, 100),
                new BlocoDeMemoria(1, 150),
                new BlocoDeMemoria(2, 200),
                new BlocoDeMemoria(3, 250),
                new BlocoDeMemoria(4, 300),
                new BlocoDeMemoria(5, 350)
        ));
        processos = new ArrayList<>();

        // Painel de entrada
        JPanel painelDeInsersao = new JPanel(new GridLayout(2, 1));
        JPanel painelDeProcesso = new JPanel();
        painelDeProcesso.add(new JLabel("Nome:"));
        campoNome = new JTextField(5);
        painelDeProcesso.add(campoNome);
        painelDeProcesso.add(new JLabel("Tamanho:"));
        campoTamanho = new JTextField(5);
        painelDeProcesso.add(campoTamanho);
        botaoDeAlocacao = new JButton("Alocar");
        painelDeProcesso.add(botaoDeAlocacao);
        botaoReset = new JButton("Reiniciar");
        painelDeProcesso.add(botaoReset);
        botaoSimularES = new JButton("Simular E/S Bloquear");
        painelDeProcesso.add(botaoSimularES);
        painelDeInsersao.add(painelDeProcesso);

        JPanel painelDeEstrategia = new JPanel();
        painelDeEstrategia.add(new JLabel("Estratégia:"));
        painelDeEstrategia.add(opcoesEstrategia);
        painelDeInsersao.add(painelDeEstrategia);

        add(painelDeInsersao, BorderLayout.NORTH);

        // Painel de memória
        painelDeMemoria = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                desenharBlocosDeMemoria(g);
            }
        };
        painelDeMemoria.setPreferredSize(new Dimension(600, 400));
        add(painelDeMemoria, BorderLayout.CENTER);

        // Lista de processos
        JList<String> listaDeProcessos = new JList<>(modeloListaDeProcessos);
        add(new JScrollPane(listaDeProcessos), BorderLayout.EAST);

        // Status da memória
        rotuloStatusMemoria = new JLabel("Memória: Total: 0KB | Ocupado: 0KB | Livre: 0KB");
        add(rotuloStatusMemoria, BorderLayout.SOUTH);

        // Ações
        botaoDeAlocacao.addActionListener(e -> {
            String nome = campoNome.getText().trim();
            int tamanho;
            try {
                tamanho = Integer.parseInt(campoTamanho.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Tamanho inválido.");
                return;
            }
            String estrategia = (String) opcoesEstrategia.getSelectedItem();
            Processo p = new Processo(nome, tamanho);
            processos.add(p);

            boolean sucesso = switch (estrategia) {
                case "primeiro encaixe" -> alocarPrimeiroEncaixe(p);
                case "melhor encaixe" -> alocarMelhorEncaixe(p);
                case "pior encaixe" -> alocarPiorEncaixe(p);
                case "próximo encaixe" -> alocarProximoEncaixe(p);
                default -> false;
            };
            if (sucesso) {
                modeloListaDeProcessos.addElement(p.toString());
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível alocar o processo.");
            }
            atualizarStatusMemoria();
        });

        botaoReset.addActionListener(e -> {
            processos.clear();
            modeloListaDeProcessos.clear();
            blocosDeMemoria.forEach(BlocoDeMemoria::liberar);
            proximoIndiceDeEncaixe = 0;
            atualizarStatusMemoria();
            repaint();
        });

        botaoSimularES.addActionListener(e -> {
            if (processos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum processo em execução.");
                return;
            }
            new Thread(() -> {
                Processo p = processos.get(new Random().nextInt(processos.size()));
                p.bloqueado = true;
                repaint();
                try {
                    Thread.sleep(3000); // Simula espera por E/S
                } catch (InterruptedException ignored) {}
                p.bloqueado = false;
                repaint();
            }).start();
        });

        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean alocarPrimeiroEncaixe(Processo p) {
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            if (bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
                bloco.alocar(p);
                return true;
            }
        }
        return false;
    }

    private boolean alocarMelhorEncaixe(Processo p) {
        BlocoDeMemoria melhor = null;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            if (bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
                if (melhor == null || bloco.tamanho < melhor.tamanho) {
                    melhor = bloco;
                }
            }
        }
        if (melhor != null) {
            melhor.alocar(p);
            return true;
        }
        return false;
    }

    private boolean alocarPiorEncaixe(Processo p) {
        BlocoDeMemoria pior = null;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            if (bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
                if (pior == null || bloco.tamanho > pior.tamanho) {
                    pior = bloco;
                }
            }
        }
        if (pior != null) {
            pior.alocar(p);
            return true;
        }
        return false;
    }

    private boolean alocarProximoEncaixe(Processo p) {
        int n = blocosDeMemoria.size();
        for (int i = 0; i < n; i++) {
            int indice = (proximoIndiceDeEncaixe + i) % n;
            BlocoDeMemoria bloco = blocosDeMemoria.get(indice);
            if (bloco.estaLivre() && bloco.tamanho >= p.tamanho) {
                bloco.alocar(p);
                proximoIndiceDeEncaixe = (indice + 1) % n;
                return true;
            }
        }
        return false;
    }

    private void desenharBlocosDeMemoria(Graphics g) {
        int y = 20;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            g.setColor(bloco.processo == null ? Color.LIGHT_GRAY : (bloco.processo.bloqueado ? Color.ORANGE : Color.GREEN));
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Bloco " + bloco.id + ": " + bloco.tamanho + "KB", 60, y + 15);
            if (bloco.processo != null) {
                g.drawString(bloco.processo.nome + " (" + bloco.processo.tamanho + "KB)", 60, y + 35);
            }
            y += 60;
        }
    }

    private void atualizarStatusMemoria() {
        int total = 0;
        int ocupado = 0;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            total += bloco.tamanho;
            if (bloco.processo != null) {
                ocupado += bloco.tamanho;
            }
        }
        int livre = total - ocupado;
        rotuloStatusMemoria.setText("Memória: Total: " + total + "KB | Ocupado: " + ocupado + "KB | Livre: " + livre + "KB");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuladorDeAlocacaoDeMemoria::new);
    }

    static class BlocoDeMemoria {
        int id, tamanho;
        Processo processo;

        BlocoDeMemoria(int id, int tamanho) {
            this.id = id;
            this.tamanho = tamanho;
        }

        boolean estaLivre() {
            return processo == null;
        }

        void alocar(Processo p) {
            this.processo = p;
        }

        void liberar() {
            this.processo = null;
        }
    }

    static class Processo {
        String nome;
        int tamanho;
        boolean bloqueado = false;

        Processo(String nome, int tamanho) {
            this.nome = nome;
            this.tamanho = tamanho;
        }

        public String toString() {
            return nome + " (" + tamanho + "KB)" + (bloqueado ? " [BLOQUEADO]" : "");
        }

    }
}

/*  public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DeadlockSimulator sim = new DeadlockSimulator();
            sim.setVisible(true);
        });
    }

    // Classes auxiliares
    static class Process {
        String name;
        List<Resource> resourcesHeld = new ArrayList<>();
        Resource waitingFor = null;

        Process(String name) {
            this.name = name;
        }
    }

    static class Resource {
        String name;
        Process allocatedTo = null;

        Resource(String name) {
            this.name = name;
        }
    }
}
*/