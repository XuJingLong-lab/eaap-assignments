package iteach.eaap.assignments.apigateway;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class SubmissionHandler {
    private WebClient.Builder builder;

    // 当学生获取自己的作业信息时，需要查看作业是否提交，以及作业的基本信息
    // 由于使用了反应式异步编程，可同时获取多项数据
    public Mono<ServerResponse> getSubmissions(ServerRequest serverRequest) {
        Flux<Submission> submissions = builder.build().get()
                .uri("http://assignments-submission/submissions")
                .exchange()
                .flatMapMany(resp -> resp.bodyToFlux(Submission.class));
        Flux<Assignment> assignments = builder.build().get()
                .uri("http://assignments-management/assignments")
                .exchange()
                .flatMapMany(resp -> resp.bodyToFlux(Assignment.class));
        submissions = submissions.map(submission -> {
            if (submission.getAssignmentId() == null) {
                return submission;
            }
            Assignment assignment = assignments
                    .filter(a -> a.getId().equals(submission.getAssignmentId()))
                    .blockFirst();
            submission.setAssignment(assignment);
            return submission;
        });

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(submissions, Submission.class));
    }

}
