package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CFG {
    // output file names
    private final static String outputDirPath = "output" + File.separator;
    public final static String allApisFilePath = outputDirPath + "allApis.json";
    public final static String apiFrequenciesFilePath = outputDirPath + "apiFrequencies.json";
    public final static String apiSequenceIndicesFilePath = outputDirPath + "apiSequenceIndices.json";
    public final static String apiSequencesFilePath = outputDirPath + "apiSequences.json";
    private final static int chunk_range = 200;
    private int chunk_i = 0;
    private int apk_lb = 0;
    private int apk_ub = 0;

    private List<Map<String, Integer>> apiFreqs = new ArrayList<>();
    private List<List<String>> apiSequences = new ArrayList<>();
    private List<List<Integer>> apiSequenceIndices = new ArrayList<>();
    private Map<String, Integer> allApis = new LinkedHashMap<>();

    private String apkPath = System.getProperty("user.dir") + File.separator + "samples";
    // for identifying dataset family
    private final static String familyKey = "a05cba79-d480-44ad-8a41-1447d544d1b";
    private File[] benignApks = null;
    private File[] maliciousApks = null;

    public CFG(String chunk_idx) {
        if (chunk_idx.isEmpty()) {
            return;
        }

        chunk_i = Integer.valueOf(chunk_idx);
        apk_lb = chunk_range * chunk_i;
        apk_ub = chunk_range * (chunk_i + 1);

        // collect apks
        this.benignApks = this.collectApks(this.apkPath + File.separator + "benign");
        // this.maliciousApks = this.collectApks(this.apkPath + File.separator + "malicious");
        File[] emptyFiles = new File[0];
        this.maliciousApks = emptyFiles;
        int allApkNum = benignApks.length + maliciousApks.length;
        apk_ub = (apk_ub > allApkNum) ? allApkNum : apk_ub;

        for (int idx = apk_lb; idx < apk_ub; ++idx) {
            apiFreqs.add(new ConcurrentHashMap<String, Integer>());
            apiSequences.add(new ArrayList<>());
            apiSequenceIndices.add(new ArrayList<>());
        }
    }

    // collectApks collects <= 4MB .apk files
    private File[] collectApks(String apkDirPath) {
        File dir = new File(apkDirPath);
        // unneeded because divided apks and others by changed directory structure
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File file, String fileName) {
                // ignore not endwith .apk and > 4MB, and .gitignore
                List<String> blackList = Arrays.asList(
                        ".gitignore",
                        "VirusShare_02e28a98d1dc5148790dc21faccf7485",
                        "VirusShare_069d20be96cb32692c6a4b0d3564c2c4",
                        "VirusShare_081b1b3673fd4200680383ba4f4d67ba",
                        "VirusShare_0de5c01c9e66be313970cc3af017f188",
                        "VirusShare_1a4ed1ca65321659b139f9cba9c9cab4",
                        "VirusShare_1407cd7c568576115204697fdbbdfa43",
                        "VirusShare_1b07a32456cc2409dcad662a9b69ae77",
                        "VirusShare_142ed5d5027f15230107415c68ff5165",
                        "VirusShare_159c9b68f6dd676c879f29f1a29425d9",
                        "VirusShare_1623a627db8b4824a21151996968f1e1",
                        "VirusShare_22c4561180fb2e9061a4cc5d1bdc307a",
                        "VirusShare_23aed3fa1fc5439d847eae7b2e5eaf58",
                        "VirusShare_23f8cb1e45937c685080eb0453ec01e4",
                        "VirusShare_24d949773062b6180a5e986b01267c39",
                        "VirusShare_25775a401a460fb85777e88d8c45927d",
                        "VirusShare_29e52d2de1cc2011eb8eacf0392d389d",
                        "VirusShare_2c5129dff9b53a7e3432371db62cb323",
                        "VirusShare_2e94d4723b8e4217d6b39aa286e3d39e",
                        "VirusShare_2ebf5292505317771bbb458c17667437",
                        "VirusShare_2f87990252304e48ebdbd421a466c0a9",
                        "VirusShare_36dd95f914b43ab5bd37507a96b29283",
                        "VirusShare_378ab0026f26a4ccbdf33f0c4403c0c4",
                        "VirusShare_37a46aec9aa86831faa3ddb6b05a05f8",
                        "VirusShare_3d15b4e43a0dc8692a905ba5612a9f10",
                        "VirusShare_4c6902001eff353631ce95febb266e3d",
                        "VirusShare_5361e076f1744c43dd65cda00bb89cc5",
                        "VirusShare_60619a013493306ce5f23286ac05f9be",
                        "VirusShare_6606e8adad40e3c5b0b8c347a38eb86b",
                        "VirusShare_98eb1f31945f4cd97088cf9fbc49d03b",
                        "VirusShare_aecb7c76cb497401be48459ff944f5fe",
                        "VirusShare_bcb3026536783bc774a05d93bc2f6039",
                        "VirusShare_c69d0d8b86bf3946ccbc011767b06919",
                        "VirusShare_ff28b758f18030c14402e100dbb6987e");
                for (String black : blackList) {
                    if (fileName.equals(black)) {
                        return false;
                    }
                }
                // if (!fileName.endsWith(".apk")) {
                // return false;
                // }
                // if (file.length() > apiSizeThreshold) {
                // return false;
                // }

                return true;
            }
        };
        File[] unsortedFiles = dir.listFiles(filter);
        Arrays.sort(unsortedFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        return unsortedFiles;
    }

    // constructCFG constructs CallFlowGraph and write the result as JSON
    public void constructCFG() {
        List<CFGConstructor> jobs = new ArrayList<>();
        int allApksNum = benignApks.length + maliciousApks.length;
        System.out.printf("All Apks: %d\n", allApksNum);
        for (int idx = apk_lb; idx < apk_ub; ++idx) {
            if (idx < maliciousApks.length) {
                jobs.add(new CFGConstructor(idx, maliciousApks[idx], 1));
            } else {
                jobs.add(new CFGConstructor(idx, benignApks[idx - maliciousApks.length], 0));
            }
        }
        jobs.stream().forEach(job -> {
            ReturnedValue res = job.call();

            Map<String, Integer> apiFreq = res.getApiFreq();
            apiFreq.put(familyKey, res.getFamily());
            this.apiFreqs.set(res.getIdx() - apk_lb, apiFreq);

            List<String> apiSequence = res.getApiSequence();
            apiSequence.add(String.valueOf(res.getFamily()));
            this.apiSequences.set(res.getIdx() - apk_lb, apiSequence);
        });

        this.deriveAllApis();
        // this.convertSignatureToIndexInSequence();
        this.writeJSON();
    }

    public void convertSignatureToIndexInSequence() {
        // signature -> index with allApis info
        for (int idx = 0; idx < apiSequences.size(); ++idx) {
            List<String> apiSequence = apiSequences.get(idx);
            List<Integer> apiSequenceIndex = new LinkedList<>();

            for (int idxJ = 0; idxJ < apiSequence.size(); ++idxJ) {
                if (idxJ == apiSequence.size() - 1) {
                    apiSequenceIndex.add(Integer.parseInt(apiSequence.get(idxJ)));
                } else {
                    apiSequenceIndex.add(this.allApis.get(apiSequence.get(idxJ)));
                }
            }

            this.apiSequenceIndices.set(idx, apiSequenceIndex);
        }
    }

    // deriveAllApis derives all system-apis with gathering sum of sets
    private void deriveAllApis() {
        Set<String> allApiSet = new HashSet<>();
        for (int idx = 0; idx < apiFreqs.size(); ++idx) {
            allApiSet.addAll(apiFreqs.get(idx).keySet());
        }
        int idx = 0;
        for (String api : allApiSet) {
            if (api.equals(familyKey)) {
                continue;
            }
            this.allApis.put(api, idx++);
        }
    }

    private void writeJSON() {
        try {
            // TODO: make a new func for meeting the following flow
            // because these operations are duplicated
            ObjectMapper objectMapper = new ObjectMapper();

            // write apiFreq
            writeJSONWithFileName(objectMapper, apiFrequenciesFilePath + "." + String.valueOf(10 + this.chunk_i), apiFreqs);
            // write allApiSets
            writeJSONWithFileName(objectMapper, allApisFilePath + "." + String.valueOf(10 + this.chunk_i), allApis);

            // NOTE: As this log file is easilly big, commented out
            // write apiSequences
            writeJSONWithFileName(objectMapper, apiSequencesFilePath + "." + String.valueOf(10 + this.chunk_i),
                    apiSequences);

            // write apiSequences coverted with index
            // writeJSONWithFileName(objectMapper,
            // apiSequenceIndicesFilePath+"."+String.valueOf(this.chunk_i),
            // apiSequenceIndices);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeJSONWithFileName(ObjectMapper mapper, String dstFilePath, Object writtenData) throws Exception {
        String jsonAsString = mapper.writeValueAsString(writtenData);
        File file = new File(dstFilePath);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(jsonAsString);
        fileWriter.close();
    }
}
