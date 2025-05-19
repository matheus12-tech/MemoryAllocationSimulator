import javax.swing.*;  // Componentes gráficos (botões, textos, painéis...)
import java.awt.*;     // Cores, layouts, desenho de formas
import java.util.*;    // Listas, Random, etc
import java.util.List;

public class SimuladorDeAlocacaoDeMemoria extends JFrame {

    // Componentes da interface
    private final JComboBox<String> opcoesEstrategia;
    private final DefaultListModel<String> modeloListaDeProcessos;

    // Estrutura de dados
    private final List<BlocoDeMemoria> blocosDeMemoria;
    private final List<Processo> processos;

    // Painéis e botões
    private final JPanel painelDeMemoria, painelEstadoProcessos;
    private final JButton botaoDeAlocacao, botaoReset, botaoSimularES;
    private final JTextField campoNome, campoTamanho, campoPrioridade, campoTempo;

    // Labels de status
    private final JLabel rotuloStatusMemoria, rotuloFalhaDePagina;
    private int falhasDePagina = 0;

    public SimuladorDeAlocacaoDeMemoria() {
        super("Simulador de Alocação de Memória"); // Título da janela
        setLayout(new BorderLayout()); // Layout principal

        // Estratégia fixa (por enquanto só paginação)
        opcoesEstrategia = new JComboBox<>(new String[] { "paginação" });

        modeloListaDeProcessos = new DefaultListModel<>();
        blocosDeMemoria = new ArrayList<>();
        for (int i = 0; i < 10; i++) blocosDeMemoria.add(new BlocoDeMemoria(i, 50)); // 10 blocos de 50KB

        processos = new ArrayList<>();

        // Interface de inserção de processo
        JPanel painelDeInsersao = new JPanel(new GridLayout(2, 1));
        JPanel painelDeProcesso = new JPanel();
        painelDeProcesso.add(new JLabel("Nome:"));
        campoNome = new JTextField(5);
        painelDeProcesso.add(campoNome);
        painelDeProcesso.add(new JLabel("Tamanho:"));
        campoTamanho = new JTextField(5);
        painelDeProcesso.add(campoTamanho);
        painelDeProcesso.add(new JLabel("Prioridade:"));
        campoPrioridade = new JTextField(3);
        painelDeProcesso.add(campoPrioridade);
        painelDeProcesso.add(new JLabel("Tempo:"));
        campoTempo = new JTextField(3);
        painelDeProcesso.add(campoTempo);

        botaoDeAlocacao = new JButton("Alocar");
        painelDeProcesso.add(botaoDeAlocacao);
        botaoReset = new JButton("Reiniciar");
        painelDeProcesso.add(botaoReset);
        botaoSimularES = new JButton("Simular E/S");
        painelDeProcesso.add(botaoSimularES);

        painelDeInsersao.add(painelDeProcesso);

        JPanel painelDeEstrategia = new JPanel();
        painelDeEstrategia.add(new JLabel("Estratégia:"));
        painelDeEstrategia.add(opcoesEstrategia);
        painelDeInsersao.add(painelDeEstrategia);

        add(painelDeInsersao, BorderLayout.NORTH);

        // Painel gráfico de memória
        painelDeMemoria = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                desenharBlocosDeMemoria(g); // Desenha RAM e disco
            }
        };
        painelDeMemoria.setPreferredSize(new Dimension(600, 500));
        add(painelDeMemoria, BorderLayout.CENTER);

        // Lista de processos
        JList<String> listaDeProcessos = new JList<>(modeloListaDeProcessos);
        add(new JScrollPane(listaDeProcessos), BorderLayout.EAST);

        // Status de memória e falhas de página
        rotuloStatusMemoria = new JLabel();
        rotuloFalhaDePagina = new JLabel("Falha de Paginas: 0");
        JPanel painelInferior = new JPanel(new GridLayout(2, 1));
        painelInferior.add(rotuloStatusMemoria);
        painelInferior.add(rotuloFalhaDePagina);
        add(painelInferior, BorderLayout.SOUTH);

        // Painel que exibe o estado atual de cada processo
        painelEstadoProcessos = new JPanel(new GridLayout(0, 1));
        add(new JScrollPane(painelEstadoProcessos), BorderLayout.WEST);

        // Clique no botão "Alocar"
        botaoDeAlocacao.addActionListener(e -> {
            try {
                // Captura os dados dos campos
                String nome = campoNome.getText().trim();
                int tamanho = Integer.parseInt(campoTamanho.getText().trim());
                int prioridade = Integer.parseInt(campoPrioridade.getText().trim());
                int tempo = Integer.parseInt(campoTempo.getText().trim());

                // Cria processo e tenta alocar
                Processo p = new Processo(nome, tamanho, prioridade, tempo);
                processos.add(p);
                p.estado = EstadoProcesso.NOVO;
                alocarPaginas(p); // Tentativa de alocação
                modeloListaDeProcessos.addElement(p.toString());
                atualizarStatusMemoria();
                atualizarEstadoProcessos();
                repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Campos inválidos.");
            }
        });

        // Clique em "Reiniciar"
        botaoReset.addActionListener(e -> {
            processos.clear();
            modeloListaDeProcessos.clear();
            blocosDeMemoria.forEach(BlocoDeMemoria::liberar);
            falhasDePagina = 0;
            atualizarStatusMemoria();
            atualizarEstadoProcessos();
            repaint();
        });

        // Clique em "Simular E/S"
        botaoSimularES.addActionListener(e -> {
            if (processos.isEmpty()) return;
            Processo p = processos.get(new Random().nextInt(processos.size()));
            new Thread(() -> {
                p.bloqueado = true;
                p.estado = EstadoProcesso.BLOQUEADO;
                atualizarEstadoProcessos();
                repaint();
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                p.bloqueado = false;
                p.estado = EstadoProcesso.PRONTO;
                atualizarEstadoProcessos();
                repaint();
            }).start();
        });

        // Thread de escalonamento por prioridade
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                synchronized (processos) {
                    processos.stream()
                            .filter(p -> p.tempoRestante > 0 && !p.bloqueado)
                            .max(Comparator.comparingInt(p -> p.prioridade))
                            .ifPresent(proximo -> {
                                proximo.emExecucao = true;
                                proximo.estado = EstadoProcesso.EXECUTANDO;
                                proximo.tempoRestante--;
                                if (proximo.tempoRestante == 0) {
                                    for (BlocoDeMemoria b : blocosDeMemoria) {
                                        if (b.processo == proximo) b.liberar();
                                    }
                                    proximo.estado = EstadoProcesso.FINALIZADO;
                                } else {
                                    proximo.estado = EstadoProcesso.PRONTO;
                                }
                                proximo.emExecucao = false;
                                SwingUtilities.invokeLater(() -> {
                                    atualizarStatusMemoria();
                                    atualizarEstadoProcessos();
                                    repaint();
                                });
                            });
                }
            }
        }).start();

        // Finaliza construção da janela
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Aloca páginas do processo, se possível. Senão: falha de página
    private void alocarPaginas(Processo p) {
        for (Pagina pg : p.paginas) {
            Optional<BlocoDeMemoria> livre = blocosDeMemoria.stream()
                    .filter(b -> b.estaLivre() && b.tamanho >= 50)
                    .findFirst();

            if (livre.isPresent()) {
                livre.get().alocar(p, pg);
            } else {
                pg.emDisco = true;
                falhasDePagina++;
            }
        }
        rotuloFalhaDePagina.setText("Falha de Paginas: " + falhasDePagina);
    }

    // Desenha blocos ocupados/livres e páginas em disco
    private void desenharBlocosDeMemoria(Graphics g) {
        int y = 20;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            g.setColor(bloco.processo == null ? Color.LIGHT_GRAY :
                    (bloco.processo.bloqueado ? Color.ORANGE :
                            (bloco.processo.emExecucao ? Color.BLUE : Color.GREEN)));
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Página " + bloco.id + ": " + bloco.tamanho + "KB", 60, y + 15);
            if (bloco.processo != null && bloco.pagina != null) {
                g.drawString(bloco.processo.nome + " P" + bloco.pagina.id, 60, y + 35);
            }
            y += 60;
        }

        // Exibe páginas em disco
        int yVirtual = y;
        for (Processo p : processos) {
            for (Pagina pg : p.paginas) {
                if (pg.emDisco) {
                    g.setColor(Color.RED);
                    g.fillRect(300, yVirtual, 200, 20);
                    g.setColor(Color.BLACK);
                    g.drawRect(300, yVirtual, 200, 20);
                    g.drawString(p.nome + " - Página " + pg.id + " (DISCO)", 310, yVirtual + 15);
                    yVirtual += 30;
                }
            }
        }
    }

    // Atualiza o label de memória (total, ocupada, livre)
    private void atualizarStatusMemoria() {
        int total = 0, ocupado = 0;
        for (BlocoDeMemoria bloco : blocosDeMemoria) {
            total += bloco.tamanho;
            if (bloco.processo != null) ocupado += bloco.tamanho;
        }
        rotuloStatusMemoria.setText("Memória: Total: " + total + "KB | Ocupado: " + ocupado + "KB | Livre: " + (total - ocupado) + "KB");
    }

    // Atualiza painel lateral com os estados dos processos
    private void atualizarEstadoProcessos() {
        painelEstadoProcessos.removeAll();
        for (Processo p : processos) {
            painelEstadoProcessos.add(new JLabel(p.nome + " - " + p.estado));
        }
        painelEstadoProcessos.revalidate();
        painelEstadoProcessos.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuladorDeAlocacaoDeMemoria::new);
    }

    // Bloco de memória de 50KB que pode conter uma página
    static class BlocoDeMemoria {
        int id, tamanho;
        Processo processo;
        Pagina pagina;

        BlocoDeMemoria(int id, int tamanho) {
            this.id = id;
            this.tamanho = tamanho;
        }

        boolean estaLivre() {
            return processo == null;
        }

        void alocar(Processo p, Pagina pg) {
            this.processo = p;
            this.pagina = pg;
            pg.naMemoria = true;
        }

        void liberar() {
            if (pagina != null) pagina.naMemoria = false;
            this.pagina = null;
            this.processo = null;
        }
    }

    // Representa um processo com páginas e tempo de execução
    static class Processo {
        String nome;
        int tamanho, prioridade, tempoRestante;
        boolean bloqueado = false, emExecucao = false;
        EstadoProcesso estado = EstadoProcesso.NOVO;
        List<Pagina> paginas = new ArrayList<>();

        Processo(String nome, int tamanho, int prioridade, int tempoRestante) {
            this.nome = nome;
            this.tamanho = tamanho;
            this.prioridade = prioridade;
            this.tempoRestante = tempoRestante;

            // Cria as páginas necessárias
            int totalPaginas = (int) Math.ceil(tamanho / 50.0);
            for (int i = 0; i < totalPaginas; i++) {
                paginas.add(new Pagina(i));
            }
        }

        public String toString() {
            return nome + " (" + tamanho + "KB, P" + prioridade + ", T=" + tempoRestante + ")" +
                    (bloqueado ? " [BLOQUEADO]" : "");
        }
    }

    // Página de um processo
    static class Pagina {
        int id;
        boolean naMemoria = false, emDisco = false;

        Pagina(int id) {
            this.id = id;
        }
    }

    // Enum com estados possíveis de um processo
    enum EstadoProcesso {
        NOVO, PRONTO, EXECUTANDO, BLOQUEADO, FINALIZADO
    }
}
